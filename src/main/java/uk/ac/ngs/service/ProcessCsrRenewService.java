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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCSException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import uk.ac.ngs.common.ConvertUtil;
import uk.ac.ngs.common.MutableConfigParams;
import uk.ac.ngs.dao.JdbcBulk_ChainDao;
import uk.ac.ngs.dao.JdbcCertificateDao;
import uk.ac.ngs.dao.JdbcRequestDao;
import uk.ac.ngs.domain.CSR_Flags;
import uk.ac.ngs.domain.CertificateRow;
import uk.ac.ngs.domain.PKCS10_RequestWrapper;
import uk.ac.ngs.domain.RequestRow;
import uk.ac.ngs.service.email.EmailService;
import uk.ac.ngs.validation.CsrRequestDbValidator;
import uk.ac.ngs.validation.CsrRequestValidationConfigParams;

import javax.inject.Inject;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Process CSR RENEW requests.
 * Intended to be called from higher level layers, e.g. from Web controllers.
 * TODO: Throw runtime exceptions that are more suitable for business layer,
 * extract interface once stable.
 *
 * @author David Meredith
 */
@Service
public class ProcessCsrRenewService {

    private static final Log log = LogFactory.getLog(ProcessCsrRenewService.class);
    private CsrManagerService csrManagerService;
    private CsrRequestValidationConfigParams csrRequestValidationConfigParams;
    private CsrRequestDbValidator csrRequestDbValidator;
    private JdbcRequestDao jdbcRequestDao;
    private JdbcBulk_ChainDao jdbcBulk_ChainDao;
    //    private String agent = "Portal";
//    private boolean emailRaOnRenew = true;
    private EmailService emailService;
    private JdbcCertificateDao certDao;
    private MutableConfigParams mutableConfigParams;

    /**
     * Process the client provided PKCKS#10 CSR RENEW request. Validates and
     * inserts a new row into the <tt>request</tt> table on success.
     *
     * @param csr          PKCS#10 Certificate Signing Request as PEM string
     * @param renewCertRow User who is renewing their certificate.
     * @param newEmail     Used to request a new email for this renewal.
     * @return Instance indicates if request succeeded or failed.
     */
    @Transactional
    public ProcessCsrResult processCsrRenew_Provided(String csr, CertificateRow renewCertRow, String newEmail) {
        return this.renewHelper(false, null, csr, renewCertRow, newEmail);
    }

    /**
     * Create a new PKCS#10 CSR RENEW request using the provided password and renewCertRow.
     * Validates and inserts a new row into the <tt>request</tt> table on success.
     *
     * @param pw           A password used to encrypt the returned PKCS#8 private key.
     * @param renewCertRow User who is renewing their certificate.
     * @param newEmail     Used to request a new email for this renewal.
     * @return Instance indicates if request succeeded or failed.
     */
    @Transactional
    public ProcessCsrResult processCsrRenew_CreateOnServer(String pw, CertificateRow renewCertRow, String newEmail) {
        return this.renewHelper(true, pw, null, renewCertRow, newEmail);
    }

    private ProcessCsrResult renewHelper(boolean createKeysOnServer, String pw, String csr,
                                         CertificateRow renewCertRow, String newEmail) {
        try {
            // Get the client cert and details
            long clientSerial = renewCertRow.getCert_key();
            String clientEmail = renewCertRow.getEmail();
            String clientDN = renewCertRow.getDn();
            String role = renewCertRow.getRole();
            log.info("Self RENEW for [" + clientSerial + "] [" + clientEmail + "] [" + role + "]");

            String ou = uk.ac.ngs.common.CertUtil.extractDnAttribute(clientDN, uk.ac.ngs.common.CertUtil.DNAttributeType.OU);
            String loc = uk.ac.ngs.common.CertUtil.extractDnAttribute(clientDN, uk.ac.ngs.common.CertUtil.DNAttributeType.L);
            String cn = uk.ac.ngs.common.CertUtil.extractDnAttribute(clientDN, uk.ac.ngs.common.CertUtil.DNAttributeType.CN);

            // Renewal may optionally request an update to the associated email address. 
            // The email value is recorded in 3 places in the 'request' table: 
            // - 'data' column value includes 'OWNEREMAIL=emailVal' in the text blob
            // - 'data' colunn value includes 'SUBJECT_ALT_NAME=emailVal' in the text blob if renew is for UKPERSON
            // - 'email' column
            // If the renewal is approved, then a new record in the certificate 
            // table will be added that includes the updated email value in the 'certifcate.email' column 
            // This is ok for a RENEWAL, because the new cert's signature will be created/bound to 
            // the new email value. 
            boolean emailUpdate = false;
            if (newEmail != null && !"".equals(newEmail.trim())) {
                log.info("Self RENEW requests an email change from [" + clientEmail + "] to [" + newEmail + "]");
                clientEmail = newEmail.trim();
                emailUpdate = true;
            }  
            /*
            boolean emailUpdate; 
            if(newEmail == null || "".equals(newEmail.trim()) ) { 
                 newEmail = clientEmail; // newEmail is same as existing email  
                 emailUpdate = false; 
            } else {
                log.info("Self RENEW requests an email change from ["+ clientEmail + "] to ["+newEmail+"]");
                emailUpdate = true; 
            }
            */


            String pkcs8StrEnc = null;
            if (createKeysOnServer) {
                X500NameBuilder x500NameBld = new X500NameBuilder(BCStyle.INSTANCE);
                x500NameBld.addRDN(BCStyle.C, csrRequestValidationConfigParams.getCountryOID());
                x500NameBld.addRDN(BCStyle.O, csrRequestValidationConfigParams.getOrgNameOID());
                x500NameBld.addRDN(BCStyle.OU, ou);
                x500NameBld.addRDN(BCStyle.L, loc);
                x500NameBld.addRDN(BCStyle.CN, cn);
                X500Name subject = x500NameBld.build();

                CsrAndPrivateKeyPemStringBuilder csrFactoryService = new CsrAndPrivateKeyPemStringBuilder();
                String[] pems = csrFactoryService.getPkcs10_Pkcs8_AsPemStrings(subject, clientEmail, pw); // change clientEmail to newEmail
                csr = pems[0];
                pkcs8StrEnc = pems[1];
            }

            // Determine renew profile
            CSR_Flags.Profile renewProfile;
            if (renewCertRow.getDn().contains(".")) {
                renewProfile = CSR_Flags.Profile.UKHOST;
            } else {
                renewProfile = CSR_Flags.Profile.UKPERSON;
            }
            // Collate
            PKCS10_RequestWrapper.Builder builder = new PKCS10_RequestWrapper.Builder(
                    CSR_Flags.Csr_Types.RENEW, renewProfile, csr, clientEmail); // change to newEmail 
            builder.setClientData(clientEmail, clientDN, clientSerial);   // keep as clientEmail 
            PKCS10_RequestWrapper csrWrapper = builder.build();

            // Validate
            if (emailUpdate && clientDN.contains("@")) {
                // Prevent renewal of a cert that contains an email in the DN if 
                // the renew requests a change of email address (because the DN 
                // is already bound to the existing email address) 
                // Temp hack: This needs to go into the validation, not here ! 
                Errors errors = new MapBindingResult(new HashMap<String, String>(), "csrWrapper");
                errors.reject("", "Can't renew a certificate and request a change of email if the certificate already contains an email in the DN");
                return new ProcessCsrResult(errors, csrWrapper);
            }
            // Note, validation for renewals compares the calling client's DN with
            // the  DN of the CSR to ensure they are the same, otherwise any valid
            // cert could be used to renew a certificate.
            Errors errors = new MapBindingResult(new HashMap<String, String>(), "csrWrapper");
            csrRequestDbValidator.validate(csrWrapper, errors);
            if (errors.hasErrors()) {
                return new ProcessCsrResult(errors, csrWrapper);
            }
            // Temporary return here while in development
            //if(true){ return "SUCCESS: CSR submitted ok [xxx]"; }

            String agent = this.mutableConfigParams.getProperty("db.request.row.data.col.version.value");
            long req_key = this.jdbcRequestDao.getNextPrimaryKey();
            RequestRow requestRow = this.createRequestRow(csrWrapper, clientEmail, role, req_key, agent); // change to newEmail 

            // If we are supporting bulk (bulk_chain and seq_bulk are supported), then map/correlate bulk ids in bulk_chain. 
            boolean doBulkChain = Boolean.parseBoolean(this.mutableConfigParams.getProperty("support.bulk.on.renew"));
            if (doBulkChain) {
                Long existingBulkId = this.jdbcRequestDao.getBulkIdForCertBy_Dn_Valid_NotExpired(requestRow.getDn());
                if (existingBulkId != null) {
                    log.info("Self RENEW is for a bulk cert oldBulkId is [" + existingBulkId + "]"); // e.g. 1234567946
                    Long newId = this.jdbcBulk_ChainDao.getCreateNewIdForOldId(existingBulkId);
                    requestRow.setBulk(newId);
                } else {
                    log.info("Self RENEW is not for a bulk cert");
                }
            }

            // Insert new request row
            this.csrManagerService.insertCsr(requestRow);
            log.info("Self RENEW created new CSR renew request [" + req_key + "] for client DN:[" + clientDN + "] serial:[" + clientSerial + "]");

            boolean emailRaOnRenew = Boolean.parseBoolean(this.mutableConfigParams.getProperty("email.ra.on.renew"));

            if (emailRaOnRenew) {
                Set<String> raEmails = new HashSet<String>(0); // use set so duplicates aren't added
                // Find all the RA email addresses, iterate and send
                List<CertificateRow> raCerts = this.certDao.findActiveRAsBy(csrWrapper.getP10Loc(), csrWrapper.getP10Ou());
                for (CertificateRow raCert : raCerts) {
                    if (raCert.getEmail() != null) {
                        raEmails.add(raCert.getEmail());
                    }
                }
                // if raEmails is empty (possible, since there may not be an RAOP 
                // for this RA anymore), then fallback to email the default list
                if (raEmails.isEmpty()) {
                    log.warn("No RAOP exits for [" + csrWrapper.getP10Loc() + " " + csrWrapper.getP10Ou() + "] emailing CA default");
                    String[] allemails = this.mutableConfigParams.getProperty("email.admin.addresses").split(",");
                    raEmails.addAll(Arrays.asList(allemails));
                }

                this.emailService.sendRaEmailOnCsrRenew(csrWrapper.getP10Req().getSubject().toString(), raEmails, req_key, emailUpdate);
            }
            return new ProcessCsrResult(req_key, csrWrapper, pkcs8StrEnc);

        } catch (IOException ex) {
            Logger.getLogger(ProcessCsrRenewService.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        } catch (InvalidKeyException ex) {
            Logger.getLogger(ProcessCsrRenewService.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(ProcessCsrRenewService.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        } catch (NoSuchProviderException ex) {
            Logger.getLogger(ProcessCsrRenewService.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        } catch (OperatorCreationException ex) {
            Logger.getLogger(ProcessCsrRenewService.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        } catch (PKCSException ex) {
            Logger.getLogger(ProcessCsrRenewService.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    private RequestRow createRequestRow(PKCS10_RequestWrapper csrWrapper,
                                        String email, String role, long req_key, String agent) throws IOException {
        // Create DB request row 
        // Note the following pre-conditions for our CA DB: 
        // requestRow.setDn() has to be in RFC2253 with this OID structure order: (CN, L, OU, O, C) 
        // PKCS10 encoded DN has to be opposite i.e. starting with C ending with CN (C, O, OU, L, CN). 
        RequestRow requestRow = new RequestRow();
        requestRow.setReq_key(req_key);
        requestRow.setFormat("PKCS#10");
        // We don't use pin for renewals, so just provide a sensible default 1234567890
        //String agent = this.mutableConfigParams.getProperty("db.request.row.data.col.version.value");  
        requestRow.setData(jdbcRequestDao.getDataColumnValue(req_key,
                csrWrapper, ConvertUtil.getHash("1234567890"), null/*certRow.getBulk()*/, agent, role));
        String dbDNFormat = uk.ac.ngs.common.CertUtil.getDbCanonicalDN(
                csrWrapper.getP10Req().getSubject().toString());
        requestRow.setDn(dbDNFormat); // needs to be RFC2253 style starting with CN. 
        requestRow.setCn(csrWrapper.getP10CN());
        requestRow.setEmail(email);
        requestRow.setRa(csrWrapper.getP10Ou() + " " + csrWrapper.getP10Loc());
        requestRow.setStatus("RENEW");
        requestRow.setRole(role);
        requestRow.setPublic_key(ConvertUtil.getDBFormattedRSAPublicKey(csrWrapper.getP10PubKey()));
        //requestRow.setRao(null);
        //requestRow.setScep_tid(null);
        //requestRow.setLoa(null);
        //requestRow.setBulk(null);
        return requestRow;
    }

    /**
     * @param csrManagerService the csrManagerService to set
     */
    @Inject
    public void setCsrManagerService(CsrManagerService csrManagerService) {
        this.csrManagerService = csrManagerService;
    }

    @Inject
    public void setCsrRequestValidationConfigParams(CsrRequestValidationConfigParams csrRequestValidationConfigParams) {
        this.csrRequestValidationConfigParams = csrRequestValidationConfigParams;
    }

    @Inject
    public void setCsrRequestDbValidator(CsrRequestDbValidator csrRequestDbValidator) {
        this.csrRequestDbValidator = csrRequestDbValidator;
    }

    /**
     * @param jdbcRequestDao the jdbcRequestDao to set
     */
    @Inject
    public void setJdbcRequestDao(JdbcRequestDao jdbcRequestDao) {
        this.jdbcRequestDao = jdbcRequestDao;
    }


    @Inject
    public void setJdbcBulk_ChainDao(JdbcBulk_ChainDao jdbcBulk_ChainDao) {
        this.jdbcBulk_ChainDao = jdbcBulk_ChainDao;
    }

    /**
     * Synchronized to allow re-setting of this property dynamically at runtime. 
     * @param agent Records the name of the software used to create the CSR. 
     */
//    public synchronized void setAgent(String agent) {
//        this.agent = agent;
//    }

    /**
     * Synchronized to allow re-setting of this property dynamically at runtime. 
     * If set to true, the EmailService is used to notify RAs of the the CSR. 
     * @param emailRaOnRenew the emailRaOnRenew to set
     */
//    public synchronized void setEmailRaOnRenew(boolean emailRaOnRenew) {
//        this.emailRaOnRenew = emailRaOnRenew;
//    }

    /**
     * @param emailService the emailService to set
     */
    @Inject
    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    @Inject
    public void setJdbcCertificateDao(JdbcCertificateDao dao) {
        this.certDao = dao;
    }

    @Inject
    public void setMutableConfigParams(MutableConfigParams mutableConfigParams) {
        this.mutableConfigParams = mutableConfigParams;
    }

}
