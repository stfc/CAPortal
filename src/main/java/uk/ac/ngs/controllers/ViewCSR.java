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
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.security.access.annotation.Secured;
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
import uk.ac.ngs.common.CertUtil;
import uk.ac.ngs.dao.JdbcCertificateDao;
import uk.ac.ngs.dao.JdbcRequestDao;
import uk.ac.ngs.domain.CertificateRow;
import uk.ac.ngs.domain.RequestRow;
import uk.ac.ngs.exceptions.IllegalCsrStateTransition;
import uk.ac.ngs.security.CaUser;
import uk.ac.ngs.security.SecurityContextService;
import uk.ac.ngs.service.CsrManagerService;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Page controller for the '/raop/viewcsr' page mapping.
 * Users with 'ROLE_RAOP' can view the CSR via a GET request and approve/delete
 * the CSR using a {@link RequestRow} form POST. Note, a user with a ROLE_RAOP can perform these
 * actions for any RA, including foreign RAs (i.e. not the RA's home RA).
 * <p>
 * This behaviour could be restricted in future so that RAs can only
 * approve/delete requests from their home RA.
 *
 * @author David Meredith
 */
@Controller
@RequestMapping("/raop/viewcsr")
@Secured("ROLE_RAOP")
public class ViewCSR {

    private static final Log log = LogFactory.getLog(ViewCSR.class);
    private SecurityContextService securityContextService;
    private JdbcRequestDao jdbcRequestDao;
    private CsrManagerService csrManagerService;
    private JdbcCertificateDao certDao;
    private final static Pattern DATA_PIN_PATTERN = Pattern.compile("PIN\\s?=\\s?(\\w+)$", Pattern.MULTILINE);
    private final static Pattern DATA_OWNERSERIAL_PATTERN = Pattern.compile("OWNERSERIAL\\s?=\\s?(\\w+)$", Pattern.MULTILINE);
    private final static Pattern DATA_NOTBEFORE_PATTERN = Pattern.compile("NOTBEFORE\\s?=\\s?(.+)$", Pattern.MULTILINE);
    private final static Pattern DATA_OWNERDN_PATTERN = Pattern.compile("OWNERDN\\s?=\\s?(.+)$", Pattern.MULTILINE);
    private final static Pattern DATA_CERT_PATTERN = Pattern.compile("-----BEGIN CERTIFICATE REQUEST-----(.+?)-----END CERTIFICATE REQUEST-----", Pattern.DOTALL);
    private final static String CSR_REQUESTROW_MODELMAPPING = "csr";
    private final static String CERTIDs_WITH_SAME_DN_MODELMAPPING = "certsWithMatchedDNs";

    @ModelAttribute
    public void populateModel(
            @RequestParam(value = "requestId", required = false) Integer requestId,
            ModelMap modelMap) {

        // initial values 
        modelMap.put(CERTIDs_WITH_SAME_DN_MODELMAPPING, new ArrayList<Long>(0));
        modelMap.put(CSR_REQUESTROW_MODELMAPPING, new RequestRow());
        modelMap.put("lastViewRefreshDate", new Date());
        modelMap.put("csr_serials_cns_with_same_bulkid", new HashMap<Long, String>(0));

        if (requestId != null) {
            //log.debug("model csr exits");
            RequestRow row = this.jdbcRequestDao.findById(requestId);
            if (row != null) {
                modelMap.put(CSR_REQUESTROW_MODELMAPPING, row);

                // find all certs existing certs that have this DN  
                Map<JdbcCertificateDao.WHERE_PARAMS, String> whereByParams =
                        new EnumMap<>(JdbcCertificateDao.WHERE_PARAMS.class);
                whereByParams.put(JdbcCertificateDao.WHERE_PARAMS.DN_LIKE, row.getDn());
                List<CertificateRow> certRows = certDao.findBy(whereByParams, null, null);

                // build lists of cert ids 
                List<Long> statusRevokedCertIds = new ArrayList<>();
                List<Long> statusValidExpiredIds = new ArrayList<>();
                List<Long> statusValidNotExpiredIds = new ArrayList<>();
                List<Long> statusOtherIds = new ArrayList<>();

                for (CertificateRow certRow : certRows) {
                    if ("REVOKED".equals(certRow.getStatus())) {
                        // add to REVOKED list 
                        statusRevokedCertIds.add(certRow.getCert_key());
                    } else if ("VALID".equals(certRow.getStatus())) {
                        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                        Date now = cal.getTime();
                        if (now.after(certRow.getNotAfter())) {
                            // add to VALID/EXPIRED list 
                            statusValidExpiredIds.add(certRow.getCert_key());
                        } else {
                            // add to VALID/NOT-EXPIRED list
                            // Note, here should never be any instances of these
                            statusValidNotExpiredIds.add(certRow.getCert_key());
                        }
                    } else {
                        // else add to unknown status list  
                        statusOtherIds.add(certRow.getCert_key());
                    }
                }
                modelMap.put("revokedCertIdsWithDN", statusRevokedCertIds);
                modelMap.put("validExpiredCertIdsWithDN", statusValidExpiredIds);
                modelMap.put("validNotExpiredCertIdsWithDN", statusValidNotExpiredIds);
                modelMap.put("otherCertIdsWithDN", statusOtherIds);

                // Does this CSR belong to a bulk? (does it have a bulk id)
                if (row.getBulk() != null) {
                    // Yes, therefore we need to lookup all the other csr rows
                    // that have the same bulkID and same Status values. 

                    // Create a map used to define our where clauses (where bulkid = '5' and status = 'valid') 
                    Map<JdbcRequestDao.WHERE_PARAMS, String> whereParams = new EnumMap<>(JdbcRequestDao.WHERE_PARAMS.class);
                    whereParams.put(JdbcRequestDao.WHERE_PARAMS.BULKID_EQ, "" + row.getBulk());
                    //whereParams.put(JdbcRequestDao.WHERE_PARAMS.STATUS_EQ, row.getStatus()); 

                    // Query the DB for related bulks
                    List<RequestRow> otherBulks = this.jdbcRequestDao.findBy(whereParams, null, null);
                    // Iterate the returned rows and extract the serial numbers/PKs and CNs of those rows
                    Map<Long, String> csrReq_keysCNsInBulk = new HashMap<>(0);
                    for (RequestRow requestRow : otherBulks) {
                        // store the serial/pk and CN in the map               
                        csrReq_keysCNsInBulk.put(requestRow.getReq_key(), requestRow.getCn());
                    }
                    // put the map into the model under the specified key
                    modelMap.put("csr_serials_cns_with_same_bulkid", csrReq_keysCNsInBulk);
                }
            }
        }
    }


    @RequestMapping(method = RequestMethod.GET)
    public void handleViewCertificate(ModelMap modelMap) {

        RequestRow row = (RequestRow) modelMap.get(CSR_REQUESTROW_MODELMAPPING);
        // Parse data col and extract values for rendering as separate model attributes 
        if (row.getData() != null) {
            Matcher pinmatcher = DATA_PIN_PATTERN.matcher(row.getData());
            if (pinmatcher.find()) {
                String pin = pinmatcher.group(1);//.toUpperCase();
                modelMap.put("pin", pin);
            }
            Matcher notbeforematcher = DATA_NOTBEFORE_PATTERN.matcher(row.getData());
            if (notbeforematcher.find()) {
                String notbefore = notbeforematcher.group(1).toUpperCase();
                modelMap.put("notbefore", notbefore);
            }
            Matcher ownerdnmatcher = DATA_OWNERDN_PATTERN.matcher(row.getData());
            if (ownerdnmatcher.find()) {
                String ownerdn = ownerdnmatcher.group(1);
                modelMap.put("ownerdn", ownerdn);
            }
            Matcher ownerserialmatcher = DATA_OWNERSERIAL_PATTERN.matcher(row.getData());
            if (ownerserialmatcher.find()) {
                String ownerserial = ownerserialmatcher.group(1);
                modelMap.put("ownerserial", ownerserial);
            }
            Matcher certmatcher = DATA_CERT_PATTERN.matcher(row.getData());
            ArrayList<String> sans = CertUtil.getSansCSR(certmatcher, row);
            if(sans != null) {
                modelMap.put("sans", sans);
            }

        } else {
            modelMap.put("pin", "");
            modelMap.put("notbefore", "");
            modelMap.put("ownerdn", "");
            modelMap.put("ownerserial", "");
        }
    }

    /**
     * Handle CSR {@link RequestRow} POSTs to "/raop/viewcsr/approve" to approve the csr.
     * Note, only the csr.req_key and csr.status need to be POSTed for processing.
     *
     * @param csr
     * @param result
     * @param redirectAttrs
     * @param sessionStatus
     * @param model
     * @param session
     * @return
     */
    @RequestMapping(value = "/approve", method = RequestMethod.POST)
    public String approveCSR(
            @ModelAttribute(CSR_REQUESTROW_MODELMAPPING) RequestRow csr, BindingResult result,
            RedirectAttributes redirectAttrs, SessionStatus sessionStatus,
            Model model, HttpSession session) {
        if (result.hasErrors()) {
            log.warn("binding and validation errors");
            return "raop/viewcsr";
        }

        // Get the current CaUser (so we can extract their DN)  
        CaUser caUser = securityContextService.getCaUserDetails();
        long cert_key = caUser.getCertificateRow().getCert_key();
        long csrSerial = csr.getReq_key();
        // Note, MUST re-fetch the csr from the DB, because the viewed csr may be stale and 
        // we don't want to trust this potentially stale status. 
        csr = this.jdbcRequestDao.findById(csrSerial);
        // check if host or user certificate (no pin for host request?)  
        StringBuilder logmessage = new StringBuilder();
        if ("NEW".equals(csr.getStatus()) || "RENEW".equals(csr.getStatus())) {
            try {
                this.csrManagerService.updateCsrStatus(csrSerial, cert_key, CsrManagerService.CSR_STATUS.APPROVED);
                logmessage.append("RA approve CSR: RAOP=[").append(cert_key).append("] CSR_ID=[").append(csrSerial).append("]");
                log.info(logmessage);
                redirectAttrs.addFlashAttribute("message", "Approval performed ok [" + csrSerial + "]");
            } catch (IllegalCsrStateTransition ex) {
                redirectAttrs.addFlashAttribute("errorMessage", ex.getMessage());
            }
        } else {
            logmessage.append("Logic error on RA CSR approve - csr does not have required state: RAOP=[")
                    .append(cert_key).append("] CSR_ID=[").append(csrSerial).append("] CSR Status=[").append(csr.getStatus()).append("]");
            log.warn(logmessage);
            redirectAttrs.addFlashAttribute("errorMessage", "Approval denied CSR ["
                    + csrSerial + "] has invalid status of [" + csr.getStatus() + "] required NEW or RENEW");
        }
        return "redirect:/raop/viewcsr?requestId=" + csrSerial;
    }


    /**
     * Handle CSR {@link RequestRow} POSTs to "/raop/viewcsr/delete" to delete the csr.
     * Note, only the csr.req_key and csr.status need to be POSTed for processing.
     *
     * @param csr
     * @param result
     * @param redirectAttrs
     * @param sessionStatus
     * @param model
     * @param session
     * @return
     */
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public String deleteCSR(
            @ModelAttribute(CSR_REQUESTROW_MODELMAPPING) RequestRow csr, BindingResult result,
            RedirectAttributes redirectAttrs, SessionStatus sessionStatus,
            Model model, HttpSession session) {
        if (result.hasErrors()) {
            log.warn("binding and validation errors");
            return "raop/viewcsr";
        }

        // Get the current CaUser (so we can extract their DN)  
        CaUser caUser = securityContextService.getCaUserDetails();
        long cert_key = caUser.getCertificateRow().getCert_key();
        long csrSerial = csr.getReq_key();
        // Note, MUST re-fetch the csr from the DB, because the viewed csr may be stale and 
        // we don't want to trust this potentially stale status. 
        csr = this.jdbcRequestDao.findById(csrSerial);
        StringBuilder logmessage = new StringBuilder();
        if ("NEW".equals(csr.getStatus()) || "RENEW".equals(csr.getStatus()) || "APPROVED".equals(csr.getStatus())) {
            try {
                this.csrManagerService.updateCsrStatus(csrSerial, cert_key, CsrManagerService.CSR_STATUS.DELETED);
                logmessage.append("RA delete CSR: RAOP=[").append(cert_key).append("] CSR_ID=[").append(csrSerial);
                logmessage.append("]");
                log.info(logmessage);
                redirectAttrs.addFlashAttribute("message", "Deletion performed ok [" + csrSerial + "]");
            } catch (IllegalCsrStateTransition ex) {
                redirectAttrs.addFlashAttribute("errorMessage", ex.getMessage());
            }
        } else {
            logmessage.append("Logic error on RA CSR delete - csr does not have required state: RAOP=[")
                    .append(cert_key).append("] CSR_ID=[").append(csrSerial).append("] CSR Status=[").append(csr.getStatus()).append("]");
            log.warn(logmessage);
            redirectAttrs.addFlashAttribute("errorMessage", "Deletion denied CSR [" + csrSerial + "]"
                    + " has invalid status of [" + csr.getStatus() + "] required NEW, RENEW or APPROVED");
        }
        return "redirect:/raop/viewcsr?requestId=" + csrSerial;
    }


    @Inject
    public void setCsrManagerService(CsrManagerService csrManagerService) {
        this.csrManagerService = csrManagerService;
    }

    @Inject
    public void setJdbcRequestDao(JdbcRequestDao jdbcRequestDao) {
        this.jdbcRequestDao = jdbcRequestDao;
    }

    @Inject
    public void setSecurityContextService(SecurityContextService securityContextService) {
        this.securityContextService = securityContextService;
    }

    @Inject
    public void setJdbcCertificateDao(JdbcCertificateDao dao) {
        this.certDao = dao;
    }
}