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
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.ac.ngs.dao.JdbcCertificateDao;
import uk.ac.ngs.domain.CertificateRow;
import uk.ac.ngs.forms.RequestDownloadCertFormBean;

import javax.inject.Inject;
import javax.validation.Valid;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author David Meredith
 */
@Controller
@RequestMapping("/pub/downloadCert")
public class DownloadCert {

    private static final Log log = LogFactory.getLog(DownloadCert.class);
    private final static Pattern DATA_CERT_PATTERN = Pattern.compile("-----BEGIN CERTIFICATE-----(.+?)-----END CERTIFICATE-----", Pattern.DOTALL);
    private JdbcCertificateDao certDao;
    private final static Pattern DATA_OWNEREMAIL_PATTERN = Pattern.compile("OWNEREMAIL\\s?=\\s?([^\\n]+)$", Pattern.MULTILINE);


    /**
     * Invoked initially to add the 'requestDownloadCertFormBean' model attribute.
     *
     * @return
     */
    @ModelAttribute("requestDownloadCertFormBean")
    public RequestDownloadCertFormBean createFormBean() {
        return new RequestDownloadCertFormBean();
    }

    /**
     * Handle GETs to '/pub/downloadCert' (i.e. the controllers base url).
     */
    @RequestMapping(method = RequestMethod.GET)
    public String handleBaseUrlGetRequest() {
        return "redirect:/pub/downloadCert/requestdownload";
    }

    /**
     * Handle GETs to '/pub/downloadCert/requestdownload' to map to
     * 'requestdownload' view.
     */
    @RequestMapping(value = "requestdownload", method = RequestMethod.GET)
    public String handleGetRequest() {
        return "pub/downloadCert/requestdownload";
    }

    @RequestMapping(value = "requestdownload", method = RequestMethod.POST)
    public String downloadAfterPost(
            @Valid @ModelAttribute("requestDownloadCertFormBean") RequestDownloadCertFormBean requestDownloadCertFormBean,
            BindingResult result, RedirectAttributes redirectAttrs, ModelMap modelMap) throws IOException {

        log.info("New cert download request");
        if (result.hasErrors()) {
            return null; // or can use: return "pub/downloadCert/requestdownload";
        }

        if (requestDownloadCertFormBean.getCertId() == null || requestDownloadCertFormBean.getEmail() == null) {
            // They haven't filled in the form correctly
            return null;
        }

        CertificateRow cert = this.certDao.findById(requestDownloadCertFormBean.getCertId());
        if (cert == null) {
            modelMap.put("errorMessage", "Can't find certificate with given ID");
            return null;
        }
        // Check the email, must be same as either 'certificate.email' column  
        // or the OWNEREMAIL in the 'certificate.data' column 
        boolean validEmailProvided = false;

        // First check against the OWNEREMAIL=someemail@world.com data col attribute
        Matcher ownerEmailMatcher = DATA_OWNEREMAIL_PATTERN.matcher(cert.getData());
        if (ownerEmailMatcher.find()) {
            String owneremail = ownerEmailMatcher.group(1);
            if (owneremail != null) {
                if (owneremail.trim().equalsIgnoreCase(requestDownloadCertFormBean.getEmail())) {
                    validEmailProvided = true;
                }
            }
        }
        // Second check against the 'certificate.email' column   
        if (requestDownloadCertFormBean.getEmail().equalsIgnoreCase(cert.getEmail())) {
            validEmailProvided = true;
        }

        if (!validEmailProvided) {
            modelMap.put("errorMessage", "Given email does not match our records");
            return null;
        }

        log.info("User Downloading: [" + cert.getCert_key() + "] [" + cert.getDn() + "]");

        // hex encoded certificate serial 
        modelMap.put("hexSerial", Long.toHexString(cert.getCert_key()));

        Matcher certmatcher = DATA_CERT_PATTERN.matcher(cert.getData());
        if (certmatcher.find()) {
            String pemString = certmatcher.group();
            modelMap.put("certdata", pemString);
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                InputStream is = new ByteArrayInputStream(pemString.getBytes(StandardCharsets.UTF_8));
                X509Certificate certObj = (X509Certificate) cf.generateCertificate(is);
                modelMap.put("certObj", certObj);
            } catch (CertificateException ex) {
                log.error(ex);
            }
        }
        modelMap.addAttribute("cert", cert);
        modelMap.addAttribute("successMessage", "Certificate [" + requestDownloadCertFormBean.getCertId() + "] Downloaded OK");
        return null;
    }

    @Inject
    public void setJdbcCertificateDao(JdbcCertificateDao dao) {
        this.certDao = dao;
    }
}
