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
package uk.ac.ngs.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import uk.ac.ngs.common.MutableConfigParams;
import uk.ac.ngs.dao.JdbcCertificateDao;
import uk.ac.ngs.domain.CertificateRow;
import uk.ac.ngs.forms.RevokeCertFormBean;
import uk.ac.ngs.security.SecurityContextService;
import uk.ac.ngs.service.email.EmailService;

/**
 * Process CSR REVOKE requests. 
 * Intended to be called from higher level layers, e.g. from Web controllers.
 * TODO: Throw runtime exceptions that are more suitable for business layer, 
 * extract interface once stable. 
 * 
 * @author David Meredith
 */
@Service
public class ProcessRevokeService {

    private static final Log log = LogFactory.getLog(ProcessRevokeService.class);
    private JdbcCertificateDao certDao;
    private CrrManagerService crrService;
    private SecurityContextService securityContextService;
    private EmailService emailService;
    private MutableConfigParams mutableConfigParams; 

    /**
     * Immutable transfer object that defines the result (success or fail) of a 
     * service layer revocation operation. 
     */
    public static class ProcessRevokeResult {

        private final Errors errors;
        private final boolean success;
        private final Long crrId;

        /**
         * Construct an instance to signify a <b>success</b>. 
         * @param crrId 
         */
        public ProcessRevokeResult(Long crrId) {
            this.success = true;
            this.errors = new MapBindingResult(new HashMap<String, String>(), "revokeRequest");
            this.crrId = crrId;
        }
        /**
         * Construct an instance to signify a <b>fail</b>. 
         * @param errors 
         */
        public ProcessRevokeResult(Errors errors) {
            this.errors = errors;
            this.success = false;
            this.crrId = null; 
        }

        public Errors getErrors() {
            return this.errors;
        }

        public boolean getSuccess() {
            return this.success;
        }

        public Long getCrrId() {
            return this.crrId;
        }
    }

    /**
     * Request a full certificate revocation (requires either a HOME RA or a CAOP).
     * If successful, a new <tt>crr</tt> row is created with status APPROVED. 
     * 
     * @param revokeCertFormBean Revoke data 
     * @param clientData Client/calling RA 
     * @return 
     */
    @Transactional
    public ProcessRevokeResult fullRevokeCertificate(
            RevokeCertFormBean revokeCertFormBean, CertificateRow clientData) {

        Errors errors = new MapBindingResult(new HashMap<String, String>(), "revokeRequest");
        long revoke_cert_key = revokeCertFormBean.getCert_key();
        long ra_cert_key = clientData.getCert_key();

        log.info("RA full revocation by: [" + ra_cert_key + "] for certificate: [" + revoke_cert_key + "]");
        log.debug("Reason: [" + revokeCertFormBean.getReason() + "]");
        CertificateRow revokeCert = this.certDao.findById(revoke_cert_key);

        if (!this.isCertRevokable(revokeCert)) {
            errors.reject("invalid.revocation.cert.notvalid", "Revocation Failed - Certificate is not VALID or has expired");
            log.warn("RA revocation failed by: [" + ra_cert_key + "] for certificate: [" + revoke_cert_key + "] - cert is not valid or has expired");
            return new ProcessRevokeResult(errors);
        }
        if (!this.canUserDoFullRevoke(revokeCert)) {
            errors.reject("invalid.revocation.permission.denied", "Revocation Failed - Only Home RAs or CA Operators can do a full revocation");
            log.warn("RA revocation failed by: [" + ra_cert_key + "] for certificate: [" + revoke_cert_key + "] - Only Home RAs or CA Operators can do a full revocation");
            return new ProcessRevokeResult(errors);
        }

        // revoke with status approved 
        long crrId = this.crrService.revokeCertificate(revoke_cert_key, ra_cert_key,
                revokeCertFormBean.getReason(), CrrManagerService.CRR_STATUS.APPROVED);
        return new ProcessRevokeResult(crrId);
    }

    /**
     * Request a certificate revocation request (i.e. any RA can request revoke, not just home RA)
     * If successful, a new <tt>crr</tt> row is created with status NEW. 
     * 
     * @param revokeCertFormBean Revoke data 
     * @param clientData Client/calling RA 
     * @return 
     * @throws java.io.IOException 
     */
    @Transactional
    public ProcessRevokeResult requestRevokeCertificate(
            RevokeCertFormBean revokeCertFormBean, CertificateRow clientData) throws IOException {

        Errors errors = new MapBindingResult(new HashMap<String, String>(), "revokeRequest");
        long revoke_cert_key = revokeCertFormBean.getCert_key();
        long ra_cert_key = clientData.getCert_key();

        log.info("RA request revocation by: [" + ra_cert_key + "] for certificate: [" + revoke_cert_key + "]");
        log.debug("Reason: [" + revokeCertFormBean.getReason() + "]");

        CertificateRow revokeCert = this.certDao.findById(revoke_cert_key);
        // Check whether this cert can actually be revoked (VALID not expired) 
        if (!this.isCertRevokable(revokeCert)) {
            errors.reject("invalid.revocation.cert.notvalid", "Revocation Failed - Certificate is not VALID or has expired");
            log.warn("RA revocation failed by: [" + ra_cert_key + "] for certificate: [" + revoke_cert_key + "] - cert is not valid or has expired");
            return new ProcessRevokeResult(errors);
        }

        // revoke with status NEW 
        long crrId = this.crrService.revokeCertificate(revoke_cert_key, ra_cert_key,
                revokeCertFormBean.getReason(), CrrManagerService.CRR_STATUS.NEW);
     
        // Email the home RAs 
        boolean emailRaOnRevoke = Boolean.parseBoolean(this.mutableConfigParams.getProperty("email.ra.on.revoke")); 
        if (emailRaOnRevoke) {
            String loc = CertUtil.extractDnAttribute(revokeCert.getDn(), CertUtil.DNAttributeType.L);
            String ou = CertUtil.extractDnAttribute(revokeCert.getDn(), CertUtil.DNAttributeType.OU);
            Set<String> raEmails = new HashSet<String>(); // use set so duplicates aren't added 
            // Find all the RA email addresses, iterate and send   
            List<CertificateRow> raCerts = this.certDao.findActiveRAsBy(loc, ou);
            for (CertificateRow raCert : raCerts) {
                if (raCert.getEmail() != null) {
                    raEmails.add(raCert.getEmail());
                }
            }

            // if raEmails is empty (possible, since there may not be an RAOP 
            // for this RA anymore), then fallback to email the default list
            if (raEmails.isEmpty()) {
                log.warn("No RAOP exits for [" + loc + " " + ou + "] emailing CA default");
                  String[] allemails = this.mutableConfigParams.getProperty("email.admin.addresses").split(",");  
                  raEmails.addAll(Arrays.asList(allemails));
            }
            this.emailService.sendRaEmailOnRevoke(revokeCert.getDn(), raEmails, crrId);
        }
        
        return new ProcessRevokeResult(crrId);
    }

    private boolean canUserDoFullRevoke(CertificateRow cert) {
        if (this.securityContextService.getCaUserDetails().getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_CAOP"))) {
            return true;
        } else {
            String viewCertDN = cert.getDn();
            String raOpDN = this.securityContextService.getCaUserDetails().getCertificateRow().getDn();
            if (CertUtil.hasSameRA(raOpDN, viewCertDN)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCertRevokable(CertificateRow cert) {
        return "VALID".equals(cert.getStatus()) && cert.getNotAfter().after(new Date());
    }

    @Inject
    public void setCrrManagerService(CrrManagerService crrService) {
        this.crrService = crrService;
    }

    @Inject
    public void setJdbcCertificateDao(JdbcCertificateDao dao) {
        this.certDao = dao;
    }

    @Inject
    public void setSecurityContextService(SecurityContextService securityContextService) {
        this.securityContextService = securityContextService;
    }

    /**
     * Synchronized to allow re-setting of this property dynamically at runtime. 
     * If set to true, the EmailService is used to notify RAs of the the revocation. 
     * @param emailRaOnRevoke the emailRaOnRenew to set
     */
//    public synchronized void setEmailRaOnRevoke(boolean emailRaOnRevoke) {
//        this.emailRaOnRevoke = emailRaOnRevoke;
//    }

    /**
     * @param emailService the emailService to set
     */
    @Inject
    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    @Inject
    public void setMutableConfigParams(MutableConfigParams mutableConfigParams){
       this.mutableConfigParams = mutableConfigParams;  
    }
}
