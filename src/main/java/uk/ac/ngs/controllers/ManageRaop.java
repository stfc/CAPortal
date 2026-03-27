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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import uk.ac.ngs.dao.JdbcCertificateDao;
import uk.ac.ngs.dao.RoleChangeRequestRepository;
import uk.ac.ngs.domain.CertificateRow;
import uk.ac.ngs.domain.RoleChangeRequest;
import uk.ac.ngs.security.CaUser;
import uk.ac.ngs.security.SecurityContextService;
import uk.ac.ngs.service.CertUtil;
import uk.ac.ngs.service.CertificateService;
import uk.ac.ngs.service.email.EmailService;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/raop/manageraop")
@Secured("ROLE_RAOP")
public class ManageRaop {

    private static final Log log = LogFactory.getLog(ManageRaop.class);
    private SecurityContextService securityContextService;
    private CertificateService certificateService;
    private JdbcCertificateDao jdbcCertificateDao;
    private RoleChangeRequestRepository roleChangeRequestRepository;
    private EmailService emailService;

    private final static Pattern DATA_PROFILE_PATTERN = Pattern.compile("PROFILE\\s?=\\s?(\\w+)$", Pattern.MULTILINE);

    public ManageRaop(SecurityContextService securityContextService,
            JdbcCertificateDao jdbcCertificateDao, CertificateService certificateService,
            RoleChangeRequestRepository roleChangeRequestRepository, EmailService emailService) {
        this.securityContextService = securityContextService;
        this.jdbcCertificateDao = jdbcCertificateDao;
        this.certificateService = certificateService;
        this.roleChangeRequestRepository = roleChangeRequestRepository;
        this.emailService = emailService;
    }


    @ModelAttribute
    public void populateModel(Model model) {
    log.debug("ManageRaop populateModel");

    // Get current user and their DN
    CaUser caUser = securityContextService.getCaUserDetails();
    String currentUserDn = caUser.getCertificateRow().getDn();

    // Extract DN attributes
    String ou = CertUtil.extractDnAttribute(currentUserDn, CertUtil.DNAttributeType.OU);
    String o = CertUtil.extractDnAttribute(currentUserDn, CertUtil.DNAttributeType.O);
    String loc = CertUtil.extractDnAttribute(currentUserDn, CertUtil.DNAttributeType.L);
    model.addAttribute("currentUserDn", currentUserDn);

    // Build RA string and add to model
    String ra = String.format("%s %s", ou, loc);
    model.addAttribute("ra", ra);
    log.debug("ra is: [" + ra + "]");

    // Fetch active users and RA operators for the current RA
    List<CertificateRow> userRaopRows = jdbcCertificateDao.findActiveUserAndRAOperatorBy(ou, o, loc);

    model.addAttribute("userRaopRows", userRaopRows);

    // To identify pending requests
    List<Long> listOfRoleChangeRequestCertKey = roleChangeRequestRepository.findAll()
            .stream()
            .map(RoleChangeRequest::getCertKey)
            .collect(Collectors.toList());

    model.addAttribute("listOfRoleChangeRequestCertKey", listOfRoleChangeRequestCertKey);

    // Add timestamp
    model.addAttribute("lastPageRefreshDate", new Date());
}


    /**
     * Select the raop/manageraop view to render.
     *
     * @return raop/manageraop
     */
    @RequestMapping(method = RequestMethod.GET)
    public String manageRaop() {
        log.debug("Controller /raop/manageraop");
        return "raop/manageraop";
    }

    /**
     * Handle POSTs to "/raop/manageraop/changeroletouser" to perform a demotion of
     * certificate to User role by a RAOP.
     * <p>
     * The view is always redirected and redirect attributes added as necessary;
     *
     * @param cert_key      key of certificate being updated
     * @param redirectAttrs
     * @return
     * @throws java.io.IOException
     */
    @RequestMapping(value = "/changeroletouser", method = RequestMethod.POST)
    public String changeRoleCertificate(@RequestParam long cert_key, RedirectAttributes redirectAttrs)
            throws IOException {
        CertificateRow targetCert = jdbcCertificateDao.findById(cert_key);
        CertificateRow currentUser = securityContextService.getCaUserDetails().getCertificateRow();
        String message; 

        if (canViewerManageRaopRole(targetCert)) {
            String newRole = "User";
            certificateService.updateCertificateRole(cert_key, newRole);

            log.info("Role change from (" + targetCert.getRole() + ") to (" + newRole + ")for certificate ("
                    + cert_key + ") by (" + currentUser.getDn() + ")");
            message = "Role updated successfully!";
            sendEmailNotificationOnRoleChangeToUser(targetCert, currentUser);     
        } else {
             message = "Role Change FAIL - user does not have correct permissions";
        }

        redirectAttrs.addFlashAttribute("responseMessage", message);
        return "redirect:/raop/manageraop";
    }

    private void sendEmailNotificationOnRoleChangeToUser(CertificateRow targetCert, CertificateRow currentUser) {
        String actorCN = currentUser.getCn();
        String actorEmail = currentUser.getEmail();
        String targetDN = targetCert.getDn();
        String targetCN = targetCert.getCn();
        String requestedEmail = targetCert.getEmail();

        // Send email to actor RAOP
        this.emailService.sendEmailOnRoleChangeToUser(actorCN, actorCN, targetDN, actorEmail);
        // Send email to user
        this.emailService.sendEmailOnRoleChangeToUser(targetCN, actorCN, targetDN, requestedEmail);
    }


    /**
     * Handle POSTs to "/raop/manageraop/requestraoprole" to request RAOP role of
     * certificate from User role by a RAOP.
     * <p>
     * The view is always redirected and redirect attributes added as necessary;
     *
     * @param cert_key      key of certificate role change being requested
     * @param redirectAttrs
     * @return
     * @throws java.io.IOException
     */
    @RequestMapping(value = "/requestraoprole", method = RequestMethod.POST)
    public String requestRaopRole(@RequestParam long cert_key, RedirectAttributes redirectAttrs)
            throws IOException {
        CertificateRow targetCert = jdbcCertificateDao.findById(cert_key);
        CertificateRow currentUser = securityContextService.getCaUserDetails().getCertificateRow();
        String message;

        if (canViewerManageRaopRole(targetCert)) {
            String newRole = "RA Operator";
            certificateService.raiseRoleChangeRequest(cert_key, targetCert, newRole, currentUser);

            log.info("Role change request from (" + targetCert.getRole() + ") to (" + newRole + ") for certificate ("
                    + cert_key + ") by (" + currentUser.getDn() + ") has been raised");
            message = "A role change request has been raised successfully";
        } else {
            message = "Role change request FAIL - user does not have correct permissions";
        }

        redirectAttrs.addFlashAttribute("responseMessage", message);
        return "redirect:/raop/manageraop";
    }


    /***
     * Check to see if a user can request role change of certificate. RAOP ONLY for own RA
     * 
     * @param targetCert certificate subject to role chnage
     * @return result of check
     */
    private boolean canViewerManageRaopRole(CertificateRow targetCert) {

        if (!hasRoleRaop()) {
            return false;
        }

        Optional<String> currentUserDnOpt = currentUserDn();
        if (currentUserDnOpt.isEmpty()) {
            log.warn("Current user's DN not available");
            return false;
        }
        String currentUserDn = currentUserDnOpt.get();
        // Check if request is for own certificate
        if (currentUserDn.equals(targetCert.getDn())) {
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

        // Only allow demotion for UKPERSON profile
        if (!"UKPERSON".equals(profile)) {
            return false;
        }

        // Compare DN attributes (OU and L) between current user and target certificate
        String currentUserOU = CertUtil.extractDnAttribute(currentUserDn, CertUtil.DNAttributeType.OU);
        String currentUserL = CertUtil.extractDnAttribute(currentUserDn, CertUtil.DNAttributeType.L);

        String targetCertOU = CertUtil.extractDnAttribute(targetCert.getDn(), CertUtil.DNAttributeType.OU);
        String targetCertL = CertUtil.extractDnAttribute(targetCert.getDn(), CertUtil.DNAttributeType.L);

        if (currentUserOU == null || currentUserL == null || targetCertOU == null || targetCertL == null) {
            log.warn("DN attribute extraction failed for certKey: " + targetCert.getCert_key());
            return false;
        }

        return currentUserOU.equals(targetCertOU) && currentUserL.equals(targetCertL);
    }

    private Optional<CaUser> currentUserDetails() {
        return Optional.ofNullable(securityContextService.getCaUserDetails());
    }

    private Optional<String> currentUserDn() {
        return currentUserDetails()
                .map(CaUser::getCertificateRow)
                .map(CertificateRow::getDn);
    }

    private boolean hasRoleRaop() {
        return currentUserDetails()
                .map(CaUser::getAuthorities)
                .map(auths -> auths.contains(new SimpleGrantedAuthority("ROLE_RAOP")))
                .orElse(false);
    }

}
