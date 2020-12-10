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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;
import uk.ac.ngs.common.MutableConfigParams;
import uk.ac.ngs.dao.JdbcRalistDao;
import uk.ac.ngs.domain.CertificateRow;
import uk.ac.ngs.domain.RalistRow;
import uk.ac.ngs.forms.NewHostCertFormBean;
import uk.ac.ngs.security.SecurityContextService;
import uk.ac.ngs.service.CertUtil;
import uk.ac.ngs.service.ProcessCsrNewService;
import uk.ac.ngs.service.ProcessCsrNewService.CsrAttributes;
import uk.ac.ngs.service.ProcessCsrResult;
import uk.ac.ngs.validation.CsrRequestValidationConfigParams;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the <tt>/cert_owner/requestHostCert</tt> page
 *
 * @author David Meredith
 */
@Controller
@RequestMapping("/cert_owner/requestHostCert")
public class CertOwnerRequestHostCert {

    private static final Log log = LogFactory.getLog(CertOwnerRequestHostCert.class);
    private JdbcRalistDao ralistDao;
    public static final String RA_ARRAY_REQUESTSCOPE = "ralistArray";
    private SecurityContextService securityContextService;
    //private Validator csrValidator;
    //private JdbcRequestDao jdbcRequestDao;
    //private CsrManagerService csrManagerService;
    private CsrRequestValidationConfigParams csrRequestValidationConfigParams;
    private MutableConfigParams mutableConfigParams;
    private ProcessCsrNewService processCsrNewService;

    @ModelAttribute
    public void populateModel(Model model, HttpSession session) throws IOException {
        //log.debug("populateModel");

        // Populate the RA list pull down 
        List<RalistRow> rows = this.ralistDao.findAllByActive(true, null, null);
        List<String> raArray = new ArrayList<String>(rows.size());

        String userDN = this.securityContextService.getCaUserDetails().getCertificateRow().getDn();
        String l = CertUtil.extractDnAttribute(userDN, CertUtil.DNAttributeType.L);
        String ou = CertUtil.extractDnAttribute(userDN, CertUtil.DNAttributeType.OU);

        // add user's RA as first option 
        if (l != null && ou != null) {
            // BUG - have had trouble submitting RA values that contain whitespace, 
            // so have replaced whitespace in ra with underscore 
            raArray.add(ou + " " + l);
        }

        for (RalistRow row : rows) {
            // BUG - have had trouble submitting RA values that contain whitespace, 
            // so have replaced whitespace in ra with underscore 
            raArray.add(row.getOu() + " " + row.getL());
        }
        model.addAttribute(RA_ARRAY_REQUESTSCOPE, raArray.toArray());

        X509Certificate cert = this.securityContextService.getCredentials();
        model.addAttribute("cert", cert);

        model.addAttribute("hostCert", cert.getSubjectDN().getName().contains("."));
        model.addAttribute("countryOID", csrRequestValidationConfigParams.getCountryOID());
        model.addAttribute("orgNameOID", csrRequestValidationConfigParams.getOrgNameOID());
        model.addAttribute("createCsrOnClientOrServer", this.mutableConfigParams.getProperty("createCsrOnClientOrServer"));
    }


    /**
     * Invoked initially to add the 'newHostCertFormBean' model attribute.
     *
     * @return
     */
    @ModelAttribute("newHostCertFormBean")
    public NewHostCertFormBean createFormBean() {
        return new NewHostCertFormBean();
    }

    /**
     * Handle GETs to '/cert_owner/requestHostCert' for Idempotent page refreshes.
     */
    @RequestMapping(method = RequestMethod.GET)
    public String handleGetRequest() {
        return "cert_owner/requestHostCert";
    }

    /**
     * Accepts POSTed CSR attributes needed to build a new PKCS#10 on the server,
     * performs validation and inserts a new row in the <tt>request</tt> table if valid.
     * Using this method requires that the CSR and the public/private keys
     * are created server-side rather than by the client.
     * <p/>
     * If the request is succeeds, 'SUCCESS' is returned appended with the
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
     * @param cn
     * @param ra
     * @param email
     * @param pw
     * @param pin
     * @return Either 'SUCCESS' or 'FAIL' which always comes at the start of the string
     * and append either the CSR/keys on success or an error message on fail.
     * @throws IOException
     */
    @RequestMapping(value = "postCsrAttributes", method = RequestMethod.POST)
    public @ResponseBody
    String submitNewCertRequestCreateCSR_KeysOnServer(
            @RequestParam String cn,
            @RequestParam String ra,
            @RequestParam String email,
            @RequestParam String pw,
            @RequestParam String pin)
            throws IOException {

        //return this.requestHostCertHelper(true, null, new CreateOnServerHelper(pw, cn, ra), email, pin);
        X509Certificate authCert = this.securityContextService.getCredentials();
        boolean isHostCert = authCert.getSubjectDN().getName().contains(".");
        if (isHostCert) {
            // should never get to this, as page only shows form/post stuff 
            // if a user cert is loaded in browser, see 'hostCert' t/f above.
            throw new IllegalArgumentException("Error - Can't request a new host certificate using a host certificate");
        }
        //return this.requestHostCertHelper(false, csr, null, email, pin);
        CertificateRow callingClient = securityContextService.getCaUserDetails().getCertificateRow();
        CsrAttributes csrAttributes = new CsrAttributes(pw, cn, ra);
        ProcessCsrResult result = this.processCsrNewService.processNewHostCSR_CreateOnServer(
                csrAttributes, callingClient, email, pin);
        return getReturnString(result, false);
    }


    /**
     * Accepts a POSTed PKCS#10 CSR request that is provided by the client,
     * performs validation and inserts a new row in the <tt>request</tt> table if valid.
     * Using this method requires that the CSR and the public/private keys
     * are created by the client rather than by the server.
     *
     * @param pin
     * @param email
     * @param csr   PKCS#10 PEM string
     * @return Either 'SUCCESS' or 'FAIL' which always comes at the start of the string
     * @throws java.io.IOException
     */
    @RequestMapping(value = "postCsr", method = RequestMethod.POST)
    public @ResponseBody
    String submitNewCertRequestCreateCSR_KeysOnClient(
            @RequestParam String pin,
            @RequestParam String email,
            @RequestParam String csr)
            throws IOException {

        X509Certificate authCert = this.securityContextService.getCredentials();
        boolean isHostCert = authCert.getSubjectDN().getName().contains(".");
        if (isHostCert) {
            // should never get to this, as page only shows form/post stuff 
            // if a user cert is loaded in browser, see 'hostCert' t/f above.
            throw new IllegalArgumentException("Error - Can't request a new host certificate using a host certificate");
        }
        //return this.requestHostCertHelper(false, csr, null, email, pin);
        CertificateRow callingClient = securityContextService.getCaUserDetails().getCertificateRow();
        ProcessCsrResult result = this.processCsrNewService.processNewHostCSR_Provided(
                csr, callingClient, email, pin);
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
    public void setRalistDao(JdbcRalistDao ralistDao) {
        this.ralistDao = ralistDao;
    }

    @Inject
    public void setSecurityContextService(SecurityContextService securityContextService) {
        this.securityContextService = securityContextService;
    }

    @Inject
    public void setCsrRequestValidationConfigParams(CsrRequestValidationConfigParams csrRequestValidationConfigParams) {
        this.csrRequestValidationConfigParams = csrRequestValidationConfigParams;
    }

    @Inject
    public void setMutableConfigParams(MutableConfigParams mutableConfigParams) {
        this.mutableConfigParams = mutableConfigParams;
    }

    /**
     * @param processCsrNewService the ProcessCsrNewService to set
     */
    @Inject
    public void setProcessCsrNewService(ProcessCsrNewService processCsrNewService) {
        this.processCsrNewService = processCsrNewService;
    }
}
