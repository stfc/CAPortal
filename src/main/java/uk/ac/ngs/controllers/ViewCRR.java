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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.ac.ngs.dao.JdbcCrrDao;
import uk.ac.ngs.domain.CrrRow;
import uk.ac.ngs.security.SecurityContextService;
import uk.ac.ngs.service.CertUtil;
import uk.ac.ngs.service.CrrManagerService;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Page controller for the '/raop/viewcrr' page mapping.
 * Users with 'ROLE_RAOP' can view the CRR via a GET request and approve/delete
 * the CRR using a {@link CrrRow} form POST. Note, a user with a ROLE_RAOP can perform these
 * actions for any RA, including foreign RAs (i.e. not the RA's home RA).
 * <p>
 * This behaviour could be restricted in future so that RAs can only
 * approve/delete requests from their home RA.
 *
 * @author David Meredith
 */
@Controller
@RequestMapping("/raop/viewcrr")
public class ViewCRR {
    private static final Log log = LogFactory.getLog(ViewCSR.class);
    private SecurityContextService securityContextService;
    private CrrManagerService crrManagerService;
    private JdbcCrrDao jdbcCrrDao;
    private final static String CRR_ROW_MODELMAPPING = "crr";


    @ModelAttribute
    public void populateDefaultModel(@RequestParam(required = false) Integer requestId,
                                     ModelMap modelMap) {

        if (requestId != null) {
            CrrRow crr = this.jdbcCrrDao.findById(requestId);
            if (crr != null) {
                List<CrrRow> rows = new ArrayList<CrrRow>(1);
                rows.add(crr);
                rows = this.jdbcCrrDao.setSubmitDateFromData(rows);
                modelMap.put(CRR_ROW_MODELMAPPING, rows.get(0));
            } else {
                modelMap.put(CRR_ROW_MODELMAPPING, new CrrRow());
            }
        } else {
            modelMap.put(CRR_ROW_MODELMAPPING, new CrrRow());
        }
        modelMap.put("canDeleteCrr", false);
        modelMap.put("canApproveCrr", false);
        modelMap.put("lastViewRefreshDate", new Date());
    }

    @RequestMapping(method = RequestMethod.GET)
    public void handleViewCRR(/*@RequestParam(required = false) Integer requestId,*/
            ModelMap modelMap) {

        CrrRow crr = (CrrRow) modelMap.get(CRR_ROW_MODELMAPPING);
        modelMap.put("canApproveCrr", this.canApproveCrr(crr));
        modelMap.put("canDeleteCrr", this.canDeleteCrr(crr));
    }

    /**
     * CAOPs and any RA can delete CRRs that have status NEW or APPROVED.
     */
    private boolean canDeleteCrr(CrrRow crr) {
        if (crr.getCrr_key() < 0) {
            return false;
        }
        // maybe enable home-RA-only approvals via configuration
        String raOpDN = this.securityContextService.getCaUserDetails().getCertificateRow().getDn();
        if (this.securityContextService.getCaUserDetails().getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_RAOP"))) {
            if (this.securityContextService.getCaUserDetails().getAuthorities()
                    .contains(new SimpleGrantedAuthority("ROLE_CAOP")) || CertUtil.hasSameRA(raOpDN, crr.getDn())) {
                return "NEW".equals(crr.getStatus()) || "APPROVED".equals(crr.getStatus());
            }
        }
        return false;
    }

    /**
     * CAOPs and any RA can approve CRRs that have status of NEW.
     */
    private boolean canApproveCrr(CrrRow crr) {
        if (crr.getCrr_key() < 0) {
            return false;
        }
        // maybe enable home-RA-only approvals via configuration
        String raOpDN = this.securityContextService.getCaUserDetails().getCertificateRow().getDn();
        if (this.securityContextService.getCaUserDetails().getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_RAOP"))) {
            if (this.securityContextService.getCaUserDetails().getAuthorities()
                    .contains(new SimpleGrantedAuthority("ROLE_CAOP")) || CertUtil.hasSameRA(raOpDN, crr.getDn())) {
                return "NEW".equals(crr.getStatus());
            }
        }
        return false;
    }

    /**
     * Handle CRR {@link CrrRow} POSTs to "/raop/viewcrr/approve" to approve the crr.
     * Note, only the crr.crr_key value needs to be populated on POST.
     */
    @RequestMapping(value = "/approve", method = RequestMethod.POST)
    public String approveCRR(
            @ModelAttribute(CRR_ROW_MODELMAPPING) CrrRow crr, BindingResult result,
            RedirectAttributes redirectAttrs, SessionStatus sessionStatus,
            Model model, HttpSession session) {
        if (result.hasErrors()) {
            log.error("binding and validation errors");
            return "raop/viewcrr";
        }
        // Get the current CaUser (so we can extract their DN)  
        long ra_cert_key = securityContextService.getCaUserDetails().getCertificateRow().getCert_key();
        long crrSerial = crr.getCrr_key();
        crr = this.jdbcCrrDao.findById(crrSerial);

        if (crr == null) {
            redirectAttrs.addFlashAttribute("errorMessage",
                    "Revocation approval denied, invalid CRR can't be found [" + crrSerial + "]");
        } else {
            if (this.canApproveCrr(crr)) {
                this.crrManagerService.approveRevocationRequest(crrSerial, ra_cert_key);
                StringBuilder logmessage = new StringBuilder();
                logmessage.append("RA approve CRR: RAOP=[").append(ra_cert_key).append("] CRR_ID=[").append(crrSerial).append("]");
                log.info(logmessage);
                redirectAttrs.addFlashAttribute("message", "Approval performed ok [" + crrSerial + "]");
            } else {
                redirectAttrs.addFlashAttribute("errorMessage",
                        "Approval denied [" + crrSerial + "]. "
                                + "Crr status is not NEW or permission required.");
            }
        }

        return "redirect:/raop/viewcrr?requestId=" + crrSerial;
    }

    /**
     * Handle CRR {@link CrrRow} POSTs to "/raop/viewcrr/delete" to delete the crr.
     * Note, only the crr.crr_key value needs to be populated on POST.
     */
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public String deleteCRR(
            @ModelAttribute(CRR_ROW_MODELMAPPING) CrrRow crr, BindingResult result,
            RedirectAttributes redirectAttrs, SessionStatus sessionStatus,
            Model model, HttpSession session) {
        if (result.hasErrors()) {
            log.error("binding and validation errors");
            return "raop/viewcrr";
        }
        // Get the current CaUser (so we can extract their DN)  
        long ra_cert_key = securityContextService.getCaUserDetails().getCertificateRow().getCert_key();
        long crrSerial = crr.getCrr_key();
        crr = this.jdbcCrrDao.findById(crrSerial);

        if (crr == null) {
            redirectAttrs.addFlashAttribute("errorMessage",
                    "Revocation approval denied, invalid CRR can't be found [" + crrSerial + "]");
        } else {
            if (this.canDeleteCrr(crr)) {
                this.crrManagerService.deleteRevocationRequest(crrSerial, ra_cert_key);
                redirectAttrs.addFlashAttribute("message", "Revocation request deleted ok [" + crrSerial + "]");
            } else {
                redirectAttrs.addFlashAttribute("errorMessage",
                        "Revocation approval denied, invalid CRR [" + crrSerial + "]. "
                                + "CRR status is not NEW or APPROVED or permmission denied.");
            }
        }
        return "redirect:/raop/viewcrr?requestId=" + crrSerial;
    }


    @Inject
    public void setSecurityContextService(SecurityContextService securityContextService) {
        this.securityContextService = securityContextService;
    }

    @Inject
    public void setJdbcRequestDao(JdbcCrrDao jdbcCrrDao) {
        this.jdbcCrrDao = jdbcCrrDao;
    }

    @Inject
    public void setCrrManagerService(CrrManagerService crrManagerService) {
        this.crrManagerService = crrManagerService;
    }
}
