/*
 * Copyright (C) 2015 STFC
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ngs.controllers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import uk.ac.ngs.dao.JdbcCertificateDao;
import uk.ac.ngs.dao.JdbcCrrDao;
import uk.ac.ngs.dao.JdbcRaopListDao;
import uk.ac.ngs.dao.JdbcRequestDao;
import uk.ac.ngs.dao.RoleChangeRequestRepository;
import uk.ac.ngs.domain.CertificateRow;
import uk.ac.ngs.domain.CrrRow;
import uk.ac.ngs.domain.RaopListRow;
import uk.ac.ngs.domain.RequestRow;
import uk.ac.ngs.domain.RoleChangeRequest;
import uk.ac.ngs.security.CaUser;
import uk.ac.ngs.security.SecurityContextService;
import uk.ac.ngs.service.CertUtil;
import uk.ac.ngs.service.CertificateService;
import uk.ac.ngs.service.email.EmailService;

import javax.inject.Inject;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

//import org.springframework.security.core.GrantedAuthority;

/**
 * @author David Meredith
 */
@Controller
@RequestMapping("/raop")
@Secured("ROLE_RAOP")
public class RaOpHome {

    private static final Log log = LogFactory.getLog(RaOpHome.class);
    private SecurityContextService securityContextService;
    private JdbcRaopListDao jdbcRaopListDao;
    private JdbcRequestDao jdbcRequestDao;
    private JdbcCrrDao jdbcCrrDao;
    private RoleChangeRequestRepository roleChangeRequestRepository;
    private CertificateService certificateService;
    private JdbcCertificateDao jdbcCertDao;
    private EmailService emailService;

    private final static Pattern DATA_PROFILE_PATTERN = Pattern.compile("PROFILE\\s?=\\s?(\\w+)$", Pattern.MULTILINE);

    public RaOpHome() {
        log.debug(RaOpHome.class.getName() + " ***created dave***");
    }


    @ModelAttribute
    public void populateModel(Model model) {
        log.debug("raop populateModel");

        // Get the current CaUser (so we can extract their DN)
        CaUser caUser = securityContextService.getCaUserDetails();

        // Extract the RA value from the user's certificate DN
        String dn = caUser.getCertificateRow().getDn();
        String OU = CertUtil.extractDnAttribute(dn, CertUtil.DNAttributeType.OU); // CLRC
        String L = CertUtil.extractDnAttribute(dn, CertUtil.DNAttributeType.L); // RAL
        String CN = CertUtil.extractDnAttribute(dn, CertUtil.DNAttributeType.CN); // meryem tahar
        String ra = OU + " " + L;
        model.addAttribute("ra", ra);
        log.debug("ra is:[" + ra + "]");

        // Get the current user's RAOP details for display (i.e. when they last
        // did the training etc). Note, this does not affect their ability to
        // do raop stuff in the portal.
        List<RaopListRow> raoprows = this.jdbcRaopListDao.findBy(OU, L, CN, true);
        log.debug("raoprows size: " + raoprows.size());
        model.addAttribute("raoprows", raoprows);

        // Fetch a list of pending CSRs for the RA (NEW and RENEW)
        // NEW
        Map<JdbcRequestDao.WHERE_PARAMS, String> whereParams = new HashMap<>();
        whereParams.put(JdbcRequestDao.WHERE_PARAMS.RA_EQ, ra);
        whereParams.put(JdbcRequestDao.WHERE_PARAMS.STATUS_EQ, "NEW");
        List<RequestRow> newRequestRows = jdbcRequestDao.findBy(whereParams, null, null);
        newRequestRows = jdbcRequestDao.setDataNotBefore(newRequestRows);
        model.addAttribute("new_reqrows", newRequestRows);
        // RENEW
        whereParams.put(JdbcRequestDao.WHERE_PARAMS.STATUS_EQ, "RENEW");
        List<RequestRow> renewRequestRows = jdbcRequestDao.findBy(whereParams, null, null);
        renewRequestRows = jdbcRequestDao.setDataNotBefore(renewRequestRows);
        model.addAttribute("renew_reqrows", renewRequestRows);

        // Fetch a list of pending CRRs for the RA
        Map<JdbcCrrDao.WHERE_PARAMS, String> crrWhereParams = new HashMap<>();
        crrWhereParams.put(JdbcCrrDao.WHERE_PARAMS.STATUS_EQ, "NEW"); // NEW,APPROVED,ARCHIVED,DELETED
        crrWhereParams.put(JdbcCrrDao.WHERE_PARAMS.DN_LIKE, "%L=" + L + ",OU=" + OU + "%");
        List<CrrRow> crrRows = jdbcCrrDao.findBy(crrWhereParams, null, null);
        log.debug("crrRows size: [" + crrRows.size() + "]");
        crrRows = jdbcCrrDao.setSubmitDateFromData(crrRows);
        model.addAttribute("crr_reqrows", crrRows);

        // Fetch list of pending RA operator role assignment requests for CAOP
        if (caUser.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_CAOP"))) {
            List<RoleChangeRequest> roleChangeRequests = roleChangeRequestRepository.findAll();
            Map<Long, String> requesterMap = roleChangeRequests.stream()
                    .map(RoleChangeRequest::getRequestedBy)
                    .distinct() // Avoid duplicate lookups
                    .map(jdbcCertDao::findById)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(
                            CertificateRow::getCert_key,
                            CertificateRow::getCn));

            model.addAttribute("requesterMap", requesterMap);
            model.addAttribute("roleChangeRequests", roleChangeRequests);
        }
        model.addAttribute("lastPageRefreshDate", new Date());
    }

    /**
     * Handle POSTs to "/raop/approverolechange" to approve a role change request of
     * certificate to RAOP role by a CAOP.
     * <p>
     * The view is always redirected and redirect attributes added as necessary;
     *
     * @param certKey      key of certificate being updated
     * @param requestId     id of the role change request 
     * @param redirectAttrs
     * @return
     * @throws java.io.IOException
     */
    @Secured("ROLE_CAOP")
    @RequestMapping(value = "/approverolechange", method = RequestMethod.POST)
    public String approverolechange(@RequestParam long certKey, @RequestParam Integer requestId,
            RedirectAttributes redirectAttrs)
            throws IOException {
        String message;
        CertificateRow requestedBy = roleChangeRequestRepository.findById(requestId)
                .map(RoleChangeRequest::getRequestedBy)
                .map(jdbcCertDao::findById)
                .orElse(null);

        try {
            CertificateRow targetCert = jdbcCertDao.findById(certKey);
            CaUser details = securityContextService.getCaUserDetails();
            CertificateRow currentUser = (details != null) ? details.getCertificateRow() : null;

            if (targetCert == null) {
                message = "Role Change FAIL - Certificate not found for certKey: " + certKey;
                log.warn(message);
            } else if (!canUserManageRaopRole(targetCert)) {
                message = "Role Change FAIL - user does not have correct permissions";
                log.warn("Unauthorized role change attempt by:" + currentUser.getDn());
            } else {
                String newRole = "RA Operator";
                certificateService.updateCertificateRole(certKey, newRole);
                roleChangeRequestRepository.deleteById(requestId);

                log.info("Role change from (" + targetCert.getRole() + ") to (" + newRole + ") for certificate ("
                        + certKey + ") by (" + currentUser.getDn() + ")");

                message = "Role updated successfully!";
                if (currentUser != null && requestedBy != null) {
                    sendEmailNotificationOnApproval(targetCert, currentUser, requestedBy);
                }
            }
        } catch (Exception e) {
            log.error("Error during role change approval for certKey " + certKey + " and requestId " + requestId + ": "
                    + e.getMessage());
            message = "Role Change FAIL - Internal error occurred";
        }

        redirectAttrs.addFlashAttribute("responseMessage", message);
        return "redirect:/raop";
    }


    /**
     * Handle POSTs to "/raop/rejectrolechange" to reject
     * certificate role change by a CAOP.
     * <p>
     * The view is always redirected and redirect attributes added as necessary;
     *
     * @param requestId      id of the request raised by RAOP
     * @param redirectAttrs
     * @return
     * @throws java.io.IOException
     */
    @Secured("ROLE_CAOP")
    @RequestMapping(value = "/rejectrolechange", method = RequestMethod.POST)
    public String rejectrolechange(@RequestParam long certKey, @RequestParam Integer requestId, RedirectAttributes redirectAttrs)
            throws IOException {
        String message;
        CertificateRow requestedBy = roleChangeRequestRepository.findById(requestId)
                .map(RoleChangeRequest::getRequestedBy)
                .map(jdbcCertDao::findById)
                .orElse(null);

        try {
            roleChangeRequestRepository.deleteById(requestId);
            message = "Request rejected successfully!";

            CertificateRow targetCert = jdbcCertDao.findById(certKey);            
            CaUser caUserDetails = securityContextService.getCaUserDetails();
            CertificateRow currentUser = (caUserDetails != null) ? caUserDetails.getCertificateRow() : null;

            if (targetCert != null && currentUser != null && requestedBy != null) {
                sendEmailNotificationOnRejection(targetCert, currentUser, requestedBy);
            }
        } catch (Exception e) {
            message = "Request rejection failed!";
            e.printStackTrace();
        }

        redirectAttrs.addFlashAttribute("responseMessage", message);
        return "redirect:/raop";
    }

    /***
     * Check to see if a user can manage certificates role change.
     * 
     * @param targetCert role change certificate
     * @return result of check
     */
    private boolean canUserManageRaopRole(CertificateRow targetCert) {
        // Check if current user has CA Operator management authority
        if (!securityContextService.getCaUserDetails().getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_CAOP"))) {
            return false;
        }

        // Validate certificate data
        if (targetCert == null || targetCert.getData() == null) {
            log.warn("Target certificate or its data is null");
            return false;
        }

        // Extract profile from certificate data
        Matcher profileMatcher = DATA_PROFILE_PATTERN.matcher(targetCert.getData());
        if (!profileMatcher.find()) {
            log.warn("Profile not found in certificate data for certKey: " + targetCert.getCert_key());
            return false;
        }

        String profile = profileMatcher.group(1);

        // Only allow role change for UKPERSON profile
        boolean isAllowed = "UKPERSON".equals(profile);
        if (!isAllowed) {
            log.info("Role change not allowed for profile:" + profile);
        }

        return isAllowed;
    }

    private void sendEmailNotificationOnApproval(CertificateRow targetCert, CertificateRow currentUser, CertificateRow requestedBy) {
        List<CertificateRow> activeCAs = this.jdbcCertDao.findActiveCAs();

        String actorCN = currentUser.getCn();
        String requesterCN = requestedBy.getCn();
        String requesterEmail = requestedBy.getEmail();
        String targetDN = targetCert.getDn();
        String targetCN = targetCert.getCn();
        String targetEmail = targetCert.getEmail();

        // Send email to admins
        for (CertificateRow ca : activeCAs) {
            String adminEmail = ca.getEmail();
            if (!adminEmail.equalsIgnoreCase(requesterEmail)) {
                this.emailService.sendEmailOnRaopRoleRequestApproval(ca.getCn() + " (CAOP)", actorCN, targetDN, adminEmail);
            }
        }
        // Send email to requester RAOP
        this.emailService.sendEmailOnRaopRoleRequestApproval(requesterCN, actorCN, targetDN, requesterEmail);
        // Send email to user
        this.emailService.sendEmailOnRaopRoleRequestApproval(targetCN, actorCN, targetDN, targetEmail);
    }

    private void sendEmailNotificationOnRejection(CertificateRow targetCert, CertificateRow currentUser, CertificateRow requestedBy) {
        List<CertificateRow> activeCAs = this.jdbcCertDao.findActiveCAs();

        String actorCN = currentUser.getCn();
        String requesterCN = requestedBy.getCn();
        String requesterEmail = requestedBy.getEmail();
        String targetDN = targetCert.getDn();
        String targetCN = targetCert.getCn();
        String targetEmail = targetCert.getEmail();

        // Send email to admins
        for (CertificateRow ca : activeCAs) {
            String adminEmail = ca.getEmail();
            if (!adminEmail.equalsIgnoreCase(requesterEmail)) {
                this.emailService.sendEmailOnRaopRoleRequestRejection(ca.getCn() + " (CAOP)", actorCN, targetDN, adminEmail);
            }
        }
        // Send email to requester RAOP
        this.emailService.sendEmailOnRaopRoleRequestRejection(requesterCN, actorCN, targetDN, requesterEmail);
        // Send email to user
        this.emailService.sendEmailOnRaopRoleRequestRejection(targetCN, actorCN, targetDN, targetEmail);
    }

    /**
     * Select the raop/home view to render.
     *
     * @return raop/home
     */
    @RequestMapping(method = RequestMethod.GET)
    public String raAdminHome() {
        log.debug("Controller /raop/");
        return "raop/raophome";
    }

    @Inject
    public void setSecurityContextService(SecurityContextService securityContextService) {
        this.securityContextService = securityContextService;
    }

    @Inject
    public void setJdbcRaopListDao(JdbcRaopListDao jdbcRaopListDao) {
        this.jdbcRaopListDao = jdbcRaopListDao;
    }

    @Inject
    public void setJdbcRequestDao(JdbcRequestDao jdbcRequestDao) {
        this.jdbcRequestDao = jdbcRequestDao;
    }

    @Inject
    public void setJdbcCrrDao(JdbcCrrDao jdbcCrrDao) {
        this.jdbcCrrDao = jdbcCrrDao;
    }

    @Inject
    public void setRoleChangeRequestRepository(RoleChangeRequestRepository roleChangeRequestRepository) {
        this.roleChangeRequestRepository = roleChangeRequestRepository;
    }

    @Inject
    public void setCertificateService(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @Inject
    public void setJdbcCertificateDao(JdbcCertificateDao jdbcCertDao) {
        this.jdbcCertDao = jdbcCertDao;
    }

    @Inject
    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }
}
