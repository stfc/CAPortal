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
 * Process Certificate Signing Requests for NEW certificates. Intended to be
 * called from higher level layers, e.g. from Web controllers. TODO: Throw
 * runtime exceptions that are more suitable for business layer, extract
 * interface once stable.
 *
 * @author David Meredith
 */
@Service
public class ProcessCsrNewService {

    private static final Log log = LogFactory.getLog(ProcessCsrNewService.class);
    private CsrManagerService csrManagerService;
    private CsrRequestValidationConfigParams csrRequestValidationConfigParams;
    private CsrRequestDbValidator csrRequestDbValidator;
    private JdbcRequestDao jdbcRequestDao;
    private EmailService emailService;
    private JdbcCertificateDao certDao;
    private MutableConfigParams mutableConfigParams;

    /**
     * Helper class used as a method argument for wrapping requested CSR
     * attributes.
     */
    public static class CsrAttributes {

        private final String pw;
        private final String cn;
        private final String ra;

        public CsrAttributes(String pw, String cn, String ra) {
            this.pw = pw;
            this.cn = cn;
            this.ra = ra;
        }
    }

    /**
     * Process a CSR NEW <b>HOST</b> request using the provided attributes to
     * build a new PKCS#10 on the server, performs validation and inserts a new
     * row in the <tt>request</tt> table on success.
     *
     * @param csrAttributes
     * @param clientData    Identifies the calling client.
     * @param email
     * @param pin
     * @return Instance indicates success or fail.
     */
    @Transactional
    public ProcessCsrResult processNewHostCSR_CreateOnServer(CsrAttributes csrAttributes,
                                                             CertificateRow clientData, String email, String pin) {

        return this.processCsrNew_Helper(true, null, csrAttributes, clientData, email, pin, CSR_Flags.Profile.UKHOST);
    }

    /**
     * Process a CSR NEW <b>USER</b> request using the provided attributes to
     * build a new PKCS#10 on the server, performs validation and inserts a new
     * row in the <tt>request</tt> table on success.
     *
     * @param csrAttributes
     * @param email
     * @param pin
     * @return Instance indicates success or fail.
     */
    @Transactional
    public ProcessCsrResult processNewUserCSR_CreateOnServer(CsrAttributes csrAttributes,
                                                             String email, String pin) {

        return this.processCsrNew_Helper(true, null, csrAttributes, null, email, pin, CSR_Flags.Profile.UKPERSON);
    }

    /**
     * Process a CSR NEW <b>HOST</b> request using the provided CSR PEM,
     * performs validation and inserts a new row in the <tt>request</tt> table
     * on success.
     *
     * @param csr        PKCS#10 PEM string.
     * @param clientData Identifies the calling client.
     * @param email
     * @param pin
     * @return Instance indicates success or fail.
     */
    @Transactional
    public ProcessCsrResult processNewHostCSR_Provided(String csr,
                                                       CertificateRow clientData, String email, String pin) {
        return this.processCsrNew_Helper(false, csr, null, clientData, email, pin, CSR_Flags.Profile.UKHOST);
    }

    /**
     * Process a CSR NEW <b>USER</b> request using the provided CSR PEM,
     * performs validation and inserts a new row in the <tt>request</tt> table
     * on success.
     *
     * @param csr   PKCS#10 PEM string.
     * @param email
     * @param pin
     * @return Instance indicates success or fail.
     */
    @Transactional
    public ProcessCsrResult processNewUserCSR_Provided(String csr,
                                                       String email, String pin) {
        ProcessCsrResult ret = this.processCsrNew_Helper(false, csr, null, null, email, pin, CSR_Flags.Profile.UKPERSON);
        return ret;

    }

    private ProcessCsrResult processCsrNew_Helper(boolean createCsrOnServer,
                                                  String csr, CsrAttributes csrAttributes,
                                                  CertificateRow clientData, String email, String pin, CSR_Flags.Profile profile) {
        try {
            if (!createCsrOnServer && csr == null) {
                throw new IllegalArgumentException("Creating CSR on client but no CSR provided");
            }
            if (createCsrOnServer && csr != null) {
                throw new IllegalArgumentException("Creating CSR on server but CSR was provided");
            }
            if (CSR_Flags.Profile.UKHOST.equals(profile) && clientData == null) {
                throw new IllegalArgumentException("New HOST csr requested but no callingClient data provided");
            }

            String pkcs8StrEnc = null;
            if (createCsrOnServer) {
                String ou = csrAttributes.ra.trim().split("\\s")[0];
                String loc = csrAttributes.ra.trim().split("\\s")[1];
                log.info("  HOST CSR created on server: Name[" + csrAttributes.cn + "] RA[" + csrAttributes.ra + "] email[" + email + "]");

                X500NameBuilder x500NameBld = new X500NameBuilder(BCStyle.INSTANCE);
                x500NameBld.addRDN(BCStyle.C, csrRequestValidationConfigParams.getCountryOID());
                x500NameBld.addRDN(BCStyle.O, csrRequestValidationConfigParams.getOrgNameOID());
                x500NameBld.addRDN(BCStyle.OU, ou);
                x500NameBld.addRDN(BCStyle.L, loc);
                x500NameBld.addRDN(BCStyle.CN, csrAttributes.cn);
                X500Name subject = x500NameBld.build();

                CsrAndPrivateKeyPemStringBuilder csrFactoryService = new CsrAndPrivateKeyPemStringBuilder();
                String[] pems = csrFactoryService.getPkcs10_Pkcs8_AsPemStrings(subject, email, csrAttributes.pw);
                csr = pems[0];
                pkcs8StrEnc = pems[1];
            } else {
                log.info("  CSR created on client");
            }

            // Collate 
            PKCS10_RequestWrapper.Builder builder = new PKCS10_RequestWrapper.Builder(
                    CSR_Flags.Csr_Types.NEW, profile, csr, email);

            // if this is a host cert request, we need the client data 
            if (CSR_Flags.Profile.UKHOST.equals(profile)) {
                String clientEmail = clientData.getEmail();
                String clientDN = clientData.getDn();
                long clientSerial = clientData.getCert_key();
                // Log request attempt 
                log.info("New HOST CSR requested by client DN:[" + clientDN + "] serial:[" + clientSerial + "]");
                builder.setClientData(clientEmail, clientDN, clientSerial);
            }
            PKCS10_RequestWrapper csrWrapper = builder.build();

            // Validate 
            Errors errors = new MapBindingResult(new HashMap<String, String>(), "csrWrapper");
            //csrRequestDbValidator.validate(csrWrapper, errors);
            this.csrRequestDbValidator.validate(csrWrapper, errors);
            if (errors.hasErrors()) {
                log.info("New Host cert failed validation: " + errors.getAllErrors().get(0).getDefaultMessage());
                // As we are reflecting user provided (untrusted) data we must:
                // Cater for XSS Prevention RULE #1 - HTML Escape Before Inserting Untrusted Data into HTML Element Content Server Side 
                // https://www.owasp.org/index.php/XSS_%28Cross_Site_Scripting%29_Prevention_Cheat_Sheet
                //return "FAIL: " + HtmlUtils.htmlEscapeHex(errors.getAllErrors().get(0).getDefaultMessage());
                return new ProcessCsrResult(errors, csrWrapper);
            }

            // Insert new request row  
            long req_key = this.jdbcRequestDao.getNextPrimaryKey();
            RequestRow requestRow = this.getRequestRow(csrWrapper, pin, email, req_key);
            //if(true){ return "SUCCESS: HOST CSR submitted ok [xxx]"; }
            this.csrManagerService.insertCsr(requestRow);
            //log.info("Created new host CSR: [" + req_key + "] for client DN:[" + clientDN + "] serial:[" + clientSerial + "]");
            log.info("Created new CSR: [" + req_key + "] ");

            // do the emails 
            this.doEmails(csrWrapper, req_key);

            return new ProcessCsrResult(req_key, csrWrapper, pkcs8StrEnc);

            // Need to thow more suitable runtime exceptions for this business tier. 
        } catch (IOException ex) {
            Logger.getLogger(ProcessCsrNewService.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(ProcessCsrNewService.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        } catch (NoSuchProviderException ex) {
            Logger.getLogger(ProcessCsrNewService.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        } catch (OperatorCreationException ex) {
            Logger.getLogger(ProcessCsrNewService.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        } catch (PKCSException ex) {
            Logger.getLogger(ProcessCsrNewService.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        } catch (InvalidKeyException ex) {
            Logger.getLogger(ProcessCsrNewService.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    private void doEmails(PKCS10_RequestWrapper csrWrapper, long req_key) throws IOException {
        List<CertificateRow> raCerts = this.certDao.findActiveRAsBy(csrWrapper.getP10Loc(), csrWrapper.getP10Ou());
        // Email the RAs
        boolean emailRaOnNew = Boolean.parseBoolean(this.mutableConfigParams.getProperty("email.ra.on.new"));
        if (emailRaOnNew) {
            Set<String> raEmails = new HashSet<String>(); // use set so duplicates aren't added 
            // Find all the RA email addresses, iterate and send   
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
            this.emailService.sendRaEmailOnCsrNew(csrWrapper.getProfile(),
                    csrWrapper.getP10Req().getSubject().toString(), raEmails, req_key);
        }

        // Email the User
        boolean emailUserOnNew = Boolean.parseBoolean(this.mutableConfigParams.getProperty("email.user.on.new"));
        if (emailUserOnNew) {
            // get list of ra operator emails and CNs 
            StringBuilder emails = new StringBuilder();
            StringBuilder cns = new StringBuilder();
            for (CertificateRow raop : raCerts) {
                emails.append(raop.getEmail()).append("; ");
                cns.append(raop.getCn()).append("; ");
            }
            if (CSR_Flags.Profile.UKHOST.equals(csrWrapper.getProfile())) {
                // String requestorCN, String requestedDN, String userEmail, String raCNs, String raEmails 
                this.emailService.sendRequestorOnNewHost(
                        CertUtil.extractDnAttribute(csrWrapper.getClientDN(), CertUtil.DNAttributeType.CN),
                        csrWrapper.getP10Req().getSubject().toString(),
                        csrWrapper.getClientEmail(), // client who submitted the host cert request
                        cns.toString(), emails.toString());
            }
            if (CSR_Flags.Profile.UKPERSON.equals(csrWrapper.getProfile())) {
                // String dn, String dn, String userEmail, String raCNs, String raEmails
                this.emailService.sendRequestorOnNewUser(
                        csrWrapper.getP10Req().getSubject().toString(),
                        csrWrapper.getEmail(), // the email entered in the form 
                        cns.toString(), emails.toString());
            }
        }
    }

    private RequestRow getRequestRow(PKCS10_RequestWrapper csrWrapper,
                                     String pin, String email, long req_key) throws IOException {
        // Create DB request row  
        // Note the following pre-conditions for our CA DB: 
        // requestRow.setDn() has to be in RFC2253 with this OID structure order: (CN, L, OU, O, C) 
        // PKCS10 encoded DN has to be opposite i.e. starting with C ending with CN (C, O, OU, L, CN). 
        RequestRow requestRow = new RequestRow();
        requestRow.setReq_key(req_key);
        requestRow.setFormat("PKCS#10");
        String agent = this.mutableConfigParams.getProperty("db.request.row.data.col.version.value");
        requestRow.setData(jdbcRequestDao.getDataColumnValue(req_key, csrWrapper, ConvertUtil.getHash(pin), null, agent, "User"));
        String dbDNFormat = uk.ac.ngs.common.CertUtil.getDbCanonicalDN(csrWrapper.getP10Req().getSubject().toString());
        requestRow.setDn(dbDNFormat); // needs to be RFC2253 starting with CN. 
        requestRow.setCn(csrWrapper.getP10CN());
        requestRow.setEmail(email);
        requestRow.setRa(csrWrapper.getP10Ou() + " " + csrWrapper.getP10Loc());  // combined "OU L" e.g. 'CLRC DL'
        requestRow.setStatus("NEW");
        requestRow.setRole("User");
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

    @Inject
    public void setJdbcRequestDao(JdbcRequestDao jdbcRequestDao) {
        this.jdbcRequestDao = jdbcRequestDao;
    }

    /**
     * Synchronized to allow re-setting of this property dynamically at runtime.
     *
     * @param agent Records the name of the software used to create the CSR.
     */
//    public synchronized void setAgent(String agent) {
//        this.agent = agent;
//    }
    /**
     * Synchronized to allow re-setting of this property dynamically at runtime.
     * If set to true, the EmailService is used to notify RAs of the CSR.
     *
     * @param emailRaOnNew the emailRaOnNew to set.
     */
//    public synchronized void setEmailRaOnNew(boolean emailRaOnNew) {
//        this.emailRaOnNew = emailRaOnNew;
//    }
    /**
     * Synchronized to allow re-setting of this property dynamically at runtime.
     * If set to true, the EmailService is used to notify the user of their CSR.
     *
     * @param emailUserOnNew
     */
//    public synchronized void setEmailUserOnNew(boolean emailUserOnNew) {
//        this.emailUserOnNew = emailUserOnNew;
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
