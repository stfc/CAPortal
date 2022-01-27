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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.HtmlUtils;
import uk.ac.ngs.common.CertUtil;
import uk.ac.ngs.common.MutableConfigParams;
import uk.ac.ngs.dao.JdbcCertificateDao;
import uk.ac.ngs.domain.CertificateRow;
import uk.ac.ngs.forms.RevokeCertFormBean;
import uk.ac.ngs.security.CaUser;
import uk.ac.ngs.security.SecurityContextService;
import uk.ac.ngs.service.CertificateService;
import uk.ac.ngs.service.CrrManagerService;
import uk.ac.ngs.service.ProcessCsrRenewService;
import uk.ac.ngs.service.ProcessCsrResult;
import uk.ac.ngs.validation.EmailValidator;

import javax.inject.Inject;
import javax.validation.Valid;
import java.io.IOException;
import java.security.cert.X509Certificate;


/**
 * Controller for processing the 'cert_owner' page. Access to this page is
 * protected by SpringSecurity and requires the 'ROLE_CERTOWNER' role. Note,
 * this role does not give any special RA/CA privileges.
 *
 * @author David Meredith
 */
@Controller
@RequestMapping("/cert_owner")
@Secured("ROLE_CERTOWNER")
public class CertOwnerBaseController {

    private static final Log log = LogFactory.getLog(CertOwnerBaseController.class);
    private SecurityContextService securityContextService;
    private CrrManagerService crrService;
    private MutableConfigParams mutableConfigParams;
    private ProcessCsrRenewService processCsrRenewService;
    private final EmailValidator emailValidator = new EmailValidator();
    private CertificateService certService;
    private JdbcCertificateDao jdbcCertDao;

    @ModelAttribute
    public void populateModel(Model model) throws IOException {
        log.debug("populateModel");
        if (this.securityContextService.getCaUserDetails() == null) {
            log.error("CaUser is null");
        }

        // Fetch the CaUser and CertificateRow entry for display in the model 
        CertificateRow certRow = this.securityContextService.getCaUserDetails().getCertificateRow();
        CaUser caUser = this.securityContextService.getCaUserDetails();
        X509Certificate cert = this.securityContextService.getCredentials();

        model.addAttribute("caUser", caUser);
        model.addAttribute("certificateRow", certRow);
        model.addAttribute("cert", cert);
        model.addAttribute("certHexSerial", cert.getSerialNumber().toString(16));
        model.addAttribute("revokeCertFormBean", new RevokeCertFormBean());

        String certDn = certRow.getDn();
        model.addAttribute("countryOID", CertUtil.extractDnAttribute(certDn, CertUtil.DNAttributeType.C));
        model.addAttribute("orgNameOID", CertUtil.extractDnAttribute(certDn, CertUtil.DNAttributeType.O));
        model.addAttribute("locOID", CertUtil.extractDnAttribute(certDn, CertUtil.DNAttributeType.L));
        model.addAttribute("ouOID", CertUtil.extractDnAttribute(certDn, CertUtil.DNAttributeType.OU));
        model.addAttribute("cnOID", CertUtil.extractDnAttribute(certDn, CertUtil.DNAttributeType.CN));
        if (certRow.getCn().contains(".") && !(certRow.getDn().contains("@"))) {
            model.addAttribute("canEditEmail", true);
        }

        model.addAttribute("createCsrOnClientOrServer", this.mutableConfigParams.getProperty("createCsrOnClientOrServer"));
    }

    /**
     * Select the cert_owner/home view to render by returning its name.
     *
     * @return cert_owner/home
     */
    @RequestMapping(method = RequestMethod.GET)
    public String certOwner() {
        log.debug("Controller /cert_owner");
        return "cert_owner/certownerhome";
    }

    /**
     * Select the cert_owner/home view to render by returning its name.
     *
     * @return cert_owner/home
     */
    @RequestMapping(value = "/home", method = RequestMethod.GET)
    public String certOwnerHome() {
        return "cert_owner/certownerhome";
    }

    /**
     * Select the cert_owner/renew view to render by returning its name.
     *
     * @return cert_owner/renew
     */
    @RequestMapping(value = "/renew", method = RequestMethod.GET)
    public String certOwnerRenew() {
        return "cert_owner/renew";
    }

    /**
     * Select the cert_owner/revoke view to render by returning its name.
     *
     * @return cert_owner/revoke
     */
    @RequestMapping(value = "/revoke", method = RequestMethod.GET)
    public String certOwnerRevoke() {
        return "cert_owner/revoke";
    }

    @RequestMapping(value = "/revoke", method = RequestMethod.POST)
    public String revokeCertificate(@Valid RevokeCertFormBean revokeCertFormBean, BindingResult result,
                                    RedirectAttributes redirectAttrs) {
        long cert_key = securityContextService.getCaUserDetails().getCertificateRow().getCert_key();
        if (result.hasErrors()) {
            log.debug("Binding validation errors on revokeCertificate");
            return "cert_owner/certownerhome";
        }

        log.info("Self revocation for [" + cert_key + "]");
        this.crrService.selfRevokeCertificate(cert_key, revokeCertFormBean.getReason());
        redirectAttrs.addFlashAttribute("revokeOkMessage", "Certificate Revoked OK");
        return "redirect:/cert_owner";
    }

    /**
     * Called when current user attempts to update the email address of their own cert.
     * A number of pre-conditions must be met:
     * <ul>
     *   <li>Cert must be a host cert (DN contains a '.' char)</li>
     *   <li>Cert must not contain an email in the DN (no '@' char in DN)</li>
     *   <li>Submitted email must be valid</li>
     * </ul>
     *
     * @param email         Newly requested email address for cert
     * @param redirectAttrs
     * @return
     * @throws java.io.IOException
     */
    @RequestMapping(value = "/changemail", method = RequestMethod.POST)
    public String changeEmail(@RequestParam String email, RedirectAttributes redirectAttrs) throws IOException {
        CertificateRow currentCert = securityContextService.getCaUserDetails().getCertificateRow();

        // this logic has been moved into certService.updateCertificateRowEmail(..)
        /*if(!currentCert.getCn().contains(".")){
            redirectAttrs.addFlashAttribute("emailUpdateFailMessage", "Cannot update email - DN is not a host certificate");
        } else if(currentCert.getDn().contains("@")){
            redirectAttrs.addFlashAttribute("emailUpdateFailMessage", "Cannot update email - DN appears to contain an email address");
        } else if(!emailValidator.validate(email)){
            redirectAttrs.addFlashAttribute("emailUpdateFailMessage", "Email update failed - invalid email address");
        } else {*/
        long cert_key = securityContextService.getCaUserDetails().getCertificateRow().getCert_key();
        log.info("Self HOST-cert email requested for Dn [" + currentCert.getDn() + "] cert_key [" + cert_key + "] old email: [" + currentCert.getEmail() + "] new email [" + email + "]");
        Errors errors = this.certService.updateCertificateRowEmail(currentCert.getDn(), cert_key, email);
        if (errors.hasErrors()) {
            StringBuilder errorMsg = new StringBuilder();
            for (ObjectError e : errors.getAllErrors()) {
                errorMsg.append(e.getDefaultMessage()).append("; ");
            }
            redirectAttrs.addFlashAttribute("emailUpdateFailMessage", errorMsg);
        } else {
            log.info("Self HOST-cert email updated OK");
            redirectAttrs.addFlashAttribute("emailUpdateOkMessage", "Email update OK");
        }

        return "redirect:/cert_owner";
    }


    /**
     * Accepts a POSTed password used to create a PKCS#10 CSR renew request on
     * the server, performs validation and inserts a new row into the <tt>request</tt>
     * table if valid.
     * <p/>
     * If the request succeeds, 'SUCCESS' is returned appended with the
     * PKCS#10 PEM string and the encrypted PKCS#8 private key PEM string.
     * If the request fails, 'FAIL' is returned appended with an error message.
     * Sample return String on success:
     * <pre>
     * SUCCESS: CSR submitted ok [1234]
     *
     * -----BEGIN CERTIFICATE REQUEST-----
     *  MIIC1zC.....blah......
     * -----END CERTIFICATE REQUEST-----
     *
     * -----BEGIN ENCRYPTED PRIVATE KEY-----
     * MIIE....blash......
     * -----END ENCRYPTED PRIVATE KEY-----
     * </pre>
     *
     * @param pw    Used to encrypt the pkcs#8 private key string.
     * @param email Used to update the email address (optional)
     * @return Either 'SUCCESS' or 'FAIL' which always comes at the start of the string
     * and append either the CSR/keys on success or an error message on fail.
     * @throws IOException
     */
    @RequestMapping(value = "/renewViaServer", method = RequestMethod.POST)
    public @ResponseBody
    String submitCertRenewalRequestCreateCSR_KeysOnServer(
            @RequestParam String pw,
            @RequestParam(value = "email", required = false) String email)
            throws IOException {

        CertificateRow clientData = securityContextService.getCaUserDetails().getCertificateRow();
        ProcessCsrResult result = this.processCsrRenewService.processCsrRenew_CreateOnServer(pw, clientData, email);
        return getReturnString(result, false);
    }

    /**
     * Accepts a POSTed PKCS#10 CSR renew request and email that is provided by the
     * client, performs validation and inserts a new row in the <tt>request</tt>
     * table if valid. Using this method requires that the CSR and the
     * public/private keys are created by the client rather than by the server.
     *
     * @param csr   PKCS#10 PEM string
     * @param email Used to update the email address (optional)
     * @return Either 'SUCCESS' or 'FAIL' which always comes at the start of the string.
     * @throws java.io.IOException
     */
    @RequestMapping(value = "/renewViaClient", method = RequestMethod.POST)
    public @ResponseBody
    String submitCertRenewalRequestCreateCSR_KeysOnClient(
            @RequestParam String csr,
            @RequestParam(value = "email", required = false) String email)
            throws IOException {

        CertificateRow clientData = securityContextService.getCaUserDetails().getCertificateRow();
        ProcessCsrResult result = this.processCsrRenewService.processCsrRenew_Provided(csr, clientData, email);
        return getReturnString(result, true);
    }


    private String getReturnString(ProcessCsrResult result, boolean csrProvided) {
        String returnResult = "";
        if (result.isSuccess()) {
            returnResult += "SUCCESS: CSR submitted ok [" + result.getReq_key() + "]";
            if (!csrProvided) {
                returnResult += "\n" + result.getPkcs8PrivateKey() + "\n" + result.getCsrWrapper().getCsrPemString();
            }
        } else {
            returnResult = "FAIL: " + HtmlUtils.htmlEscapeHex(result.getErrors().getAllErrors().get(0).getDefaultMessage());
        }
        return returnResult;
    }

    @Inject
    public void setSecurityContextService(SecurityContextService securityContextService) {
        this.securityContextService = securityContextService;
    }

    @Inject
    public void setCrrManagerService(CrrManagerService crrService) {
        this.crrService = crrService;
    }

    @Inject
    public void setMutableConfigParams(MutableConfigParams mutableConfigParams) {
        this.mutableConfigParams = mutableConfigParams;
    }

    /**
     * @param processCsrRenewService the processCsrRenewService to set
     */
    @Inject
    public void setProcessCsrRenewService(ProcessCsrRenewService processCsrRenewService) {
        this.processCsrRenewService = processCsrRenewService;
    }

    @Inject
    public void setCertificateService(CertificateService certService) {
        this.certService = certService;
    }

    @Inject
    public void setJdbcCertificateDao(JdbcCertificateDao jdbcCertDao) {
        this.jdbcCertDao = jdbcCertDao;
    }

}
