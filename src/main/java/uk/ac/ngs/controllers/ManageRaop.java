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
import uk.ac.ngs.domain.CertificateRow;
import uk.ac.ngs.security.CaUser;
import uk.ac.ngs.security.SecurityContextService;
import uk.ac.ngs.service.CertUtil;
import uk.ac.ngs.service.CertificateService;

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

    private final static Pattern DATA_PROFILE_PATTERN = Pattern.compile("PROFILE\\s?=\\s?(\\w+)$", Pattern.MULTILINE);

    public ManageRaop(SecurityContextService securityContextService,
            JdbcCertificateDao jdbcCertificateDao, CertificateService certificateService) {
        this.securityContextService = securityContextService;
        this.jdbcCertificateDao = jdbcCertificateDao;
        this.certificateService = certificateService;
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

    // Build RA string and add to model
    String ra = String.format("%s %s", ou, loc);
    model.addAttribute("ra", ra);
    log.debug("ra is:[" + ra + "]");

    // Fetch active users and RA operators for the current RA
    List<CertificateRow> userRaopRows = jdbcCertificateDao.findActiveUserAndRAOperatorBy(ou, o, loc);

    // Exclude current user from the list
    List<CertificateRow> filteredRows = userRaopRows.stream()
        .filter(row -> !currentUserDn.equals(row.getDn()))
        .collect(Collectors.toList());

    log.debug("Filtered User and RA Operator rows size: " + filteredRows.size());
    model.addAttribute("userRaopRows", filteredRows);

    // Add timestamp
    model.addAttribute("lastPageRefreshDate", new Date());
}


    /**
     * Select the raop/manageraop view to render.
     *
     * @return raop/manageraop
     */
    @RequestMapping(method = RequestMethod.GET)
    public String manageRaop(Locale locale, Model model) {
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

        if (viewerCanDemote(currentUser)) {
            String newRole = "User";
            certificateService.updateCertificateRole(cert_key, newRole);

            log.info("Role change from (" + targetCert.getRole() + ") to (" + newRole + ")for certificate ("
                    + cert_key + ") by (" + currentUser.getDn() + ")");
            message = "Role Change OK";           
        } else {
             message = "Role Change FAIL - Viewer does not have correct permissions";
        }

        redirectAttrs.addFlashAttribute("responseMessage", message);
        //redirectAttrs.addAttribute("certId", cert_key);
        return "redirect:/raop/manageraop";
    }


    /***
     * Check to see if a user can demote certificates. RA-OP ONLY for own RA
     * 
     * @param viewCert certificate of the viewer
     * @return result of check
     */
    private boolean viewerCanDemote(CertificateRow cert) {
        // Check if current user has RA Operator role
        if (!securityContextService.getCaUserDetails().getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_RAOP"))) {
            return false;
        }

        // Extract profile from certificate data
        String profile = "default";
        Matcher profileMatcher = DATA_PROFILE_PATTERN.matcher(cert.getData());
        if (profileMatcher.find()) {
            profile = profileMatcher.group(1);
        }

        // Only allow demotion for UKPERSON profile
        if (!"UKPERSON".equals(profile)) {
            return false;
        }

        // Compare DN attributes (OU and L) between current user and target certificate
        String currentUserDn = securityContextService.getCaUserDetails().getCertificateRow().getDn();
        String currentOU = CertUtil.extractDnAttribute(currentUserDn, CertUtil.DNAttributeType.OU);
        String currentL = CertUtil.extractDnAttribute(currentUserDn, CertUtil.DNAttributeType.L);

        String certOU = CertUtil.extractDnAttribute(cert.getDn(), CertUtil.DNAttributeType.OU);
        String certL = CertUtil.extractDnAttribute(cert.getDn(), CertUtil.DNAttributeType.L);

        return currentOU.equals(certOU) && currentL.equals(certL);
    }
}
