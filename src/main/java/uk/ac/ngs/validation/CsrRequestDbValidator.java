/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.ngs.validation;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ngs.common.CertUtil;
import uk.ac.ngs.common.ConvertUtil;
import uk.ac.ngs.common.MutableConfigParams;
import uk.ac.ngs.dao.JdbcCertificateDao;
import uk.ac.ngs.dao.JdbcRequestDao;
import uk.ac.ngs.domain.CSR_Flags;
import uk.ac.ngs.domain.CSR_Flags.Csr_Types;
import uk.ac.ngs.domain.CSR_Flags.Profile;
import uk.ac.ngs.domain.PKCS10_RequestWrapper;

/**
 * Top level  {@link Validator} for PKCS10 CSR requests. 
 * This class invokes the collaborators passed in the constructor 
 * and should be called before insertion of new CSRs into the DB.  
 * A number of preconditions must be satisfied before the CSR can be inserted 
 * which vary according to the type of CSR wrapped by {@link PKCS10_RequestWrapper}.   
 * 
 * Typical usage outside of the Spring context requires you create an
 * {@link org.springframework.validation.Errors} implementation. You can use the
 * Spring MapBindingResult as shown below:  
 * <code>
 * CsrRequestDbValidator validator = new CsrRequestDbValidator(dnValidator, csrValidator);
 * Errors errors = new MapBindingResult(new HashMap<String, String>(), "csrPemStrRequest");
 * validator.validate(csrWrapper, errors);
 * if(errors.hasErrors()) { ...do something  }
 * </code>
 *
 * @author David Meredith
 */
public class CsrRequestDbValidator implements Validator {

    private static final Log log = LogFactory.getLog(CsrRequestDbValidator.class);
    private final PKCS10Parser csrParser = new PKCS10Parser(); 
    private JdbcRequestDao jdbcRequestDao;
    private JdbcCertificateDao jdbcCertificateDao; 
    private Validator csrValidator; 
    private Validator dnValidator; 
    private final EmailValidator emailValidator = new EmailValidator();
    private MutableConfigParams mutableConfigParams; 


    /**
     * Construct a new instance by providing the collaborator {@link Validator} instances. 
     * <tt>setJdbcRequestDao()</tt> and <tt>setJdbcCertificateDao()</tt>  
     * must also be called as these are required collaborators. 
     * 
     * @param dnValidator Must support validation of {@link PKCS10_RequestWrapper} instances. 
     * @param csrValidator Must support validation of CSR PEM {@link String} instances.  
     */
    public CsrRequestDbValidator(Validator dnValidator, Validator csrValidator){
        if(dnValidator == null || !dnValidator.supports(PKCS10_RequestWrapper.class)){
            throw new IllegalArgumentException(
                    "The supplied [Validator] must support the validation of [PKCS10_RequestWrapper] instances."); 
        }
        if(csrValidator == null || !csrValidator.supports(String.class)){
            throw new IllegalArgumentException(
                    "The supplied [Validator] must support the validation of PEM [String] instances."); 
        }
        this.dnValidator = dnValidator; 
        this.csrValidator = csrValidator; 
    }
    
    @Override
    public boolean supports(Class<?> type) {
        return PKCS10_RequestWrapper.class.equals(type);
    }

    @Override
    public void validate(Object o, Errors errors) {
        PKCS10_RequestWrapper requestWrapper = (PKCS10_RequestWrapper) o;
        String csrPemString = requestWrapper.getCsrPemString();

        // parse the pemString
        PKCS10CertificationRequest req = csrParser.parseCsrPemString(csrPemString);
        if (req == null) {
            errors.reject("pkcs10.validation.ioexception", "Invalid PKCS10 - unable to read pem string");
            return;
        }

        // Get the jcaRequest and public key for later use 
        JcaPKCS10CertificationRequest jcaReq = new JcaPKCS10CertificationRequest(req);
        PublicKey pubkey;
        try {
            pubkey = jcaReq.getPublicKey();
        } catch (InvalidKeyException ex) {
            Logger.getLogger(CsrRequestDbValidator.class.getName()).log(Level.SEVERE, null, ex);
            errors.reject("pkcs10.validation.invalidkeyspecexception", "Invalid PKCS10 key");
            return;
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(CsrRequestDbValidator.class.getName()).log(Level.SEVERE, null, ex);
            errors.reject("pkcs10.validation.nosuchalgorithmException", "Invalid PKCS10 No Such Algorithm");
            return;
        }

        // Do validation common to any type of csr request 
        this.validateClientDataInvariants(errors, requestWrapper);
        this.validateCommon(errors, requestWrapper, pubkey, req);
        if (errors.hasErrors()) {
            return;
        }

        // Validate according to which type of csr request 
        if(requestWrapper.getCsr_type().equals(CSR_Flags.Csr_Types.NEW)){
           this.validateCSR_NEW(errors, requestWrapper, req); 
           
        } else if(requestWrapper.getCsr_type().equals(CSR_Flags.Csr_Types.RENEW)){
            this.validateCSR_RENEW(errors, requestWrapper, req); 
            
        } else {
            throw new IllegalStateException("Error - Unknown Csr_Types"); 
        }
        if (errors.hasErrors()) {
            return;
        }

    }

    /**
     * Validate the client data invariants for NEW/RENEW UKHOST. 
     * @param errors
     * @param csrWrapper 
     */
    private void validateClientDataInvariants(Errors errors, PKCS10_RequestWrapper csrWrapper) {
        // ClientData is required For a NEW UKHOST request 
        if ((CSR_Flags.Profile.UKHOST.equals(csrWrapper.getProfile())
                && CSR_Flags.Csr_Types.NEW.equals(csrWrapper.getCsr_type()))
                || // ClientData is also required for a RENEW request
                (CSR_Flags.Csr_Types.RENEW.equals(csrWrapper.getCsr_type()))) {

            if (csrWrapper.getClientDN() == null) {
                // Note, vaildation of DN is not strictly needed here as its
                // performed by the injected dnValidator, but this check 
                // maintains clientData invarient requirements. 
                errors.reject("pkcs10.validation.request.invalid.clientDN", 
                        "Invalid ClientData DN is null or invalid");
            }
            if (!emailValidator.validate(csrWrapper.getClientEmail())) {
                errors.reject("pkcs10.validation.request.invalid.clientEmail", 
                        "Invalid ClientEmail");
            }
            if (!(csrWrapper.getClientSerial() >= 0)) {
                errors.reject("pkcs10.validation.request.invalid.clientSerial", 
                        "Invalid ClientSerial is invalid");
            }
        }
    }


    private void validateCSR_RENEW(Errors errors, PKCS10_RequestWrapper requestWrapper, 
            PKCS10CertificationRequest req){
        
        // Up to this point, user has passed either SSL or PPPK client auth using 
        // an existing VALID/non-expired cert (requestWrapper.clientData). 
        // 
        // You can only pass renewal validation below if this VALID cert has the SAME canonical DN as 
        // the PKCS10's DN (this DN check is done below), otherwise you could pass 
        // SSL client auth using one valid cert but request renewal of any other cert. 
        // 
        // Therefore, for a renewals, you need to authenticate with the valid/non-expired 
        // cert that is targeted for renewal - including bulk host renwals which are issued one by one. 
        // (Note, this pre-condition may? not hold for future renewals requested by RAs on behalf of another cert?) 
        // 
        // To do this, check that the canonical DN of the PKCS10 is the same 
        // as the PPPK auth cert's canonical DN. Without this check, 
        // they have only proven that they have authenticated using *some valid* certificate.  
        // 
        // Here, clientDN has been set by reading from the 'certificate.dn' db table col
        // and has an RFC2253 style value akin to: 
        // 'emailAddress=someEmail@world.com,CN=david meredith,L=DL,OU=CLRC,O=eScience,C=UK'
        // (emailAddress may/not be present). Therefore, 
        // excluding the optional leading email attribute(s), the format of the 
        // clientDN is RFC2253 (comma separators with no spaces) which means the 
        // clientDN MUST ALWAYS contain the csrRFC2253DN. 
       
        // Get the Subject DN string with following structure order: (CN, L, OU, O, C)  
        String csrRFC2253DN = CertUtil.getDbCanonicalDN(req.getSubject().toString());
        
        if(!requestWrapper.getClientDN().toLowerCase(Locale.ENGLISH).contains(
                csrRFC2253DN.toLowerCase(Locale.ENGLISH))){
            errors.reject("pkcs10.validation.request.renew.csrdn.notmatch.clientdn", 
                    "Invalid Renew - Renew CSR DN does not match the clients certificate DN");
            return; 
        }
        // Check that a valid and non-expired certificate exists in our CA DB 
        // with the specified clientDN (i.e. the auth cert's DN - can't renew imaginary certificate). 
        // Note, we do not check for VALID rows in the certificate table using 
        // RFC 1779 style DN (as in the 'does PKCS#10 request already exist' check). 
        // This is because all DNs in the cert table are in a RFC2253 style (i.e. 
        // comma-separated with no spaces - but host cert DNs may be pre-pended 
        // with emailAddress=<value>) 
        if( 0 >= this.jdbcCertificateDao.countByStatusIsVALIDNotExpired(csrRFC2253DN, 0)){
            errors.reject("pkcs10.validation.request.renew.cert.notexist", 
                    "Invalid Renew - A valid and non-expired certificate with specified DN does not exist"); 
            return; 
        }

        // Are we supporting renewal of bulk certificates? (requires bulk_chain, seq_bulk to be deployed)  
        boolean doBulkChain; 
        try { 
            doBulkChain = Boolean.parseBoolean(this.mutableConfigParams.getProperty("support.bulk.on.renew"));
        } catch(IOException ex){
            Logger.getLogger(CsrRequestDbValidator.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);   
        }
        if(!doBulkChain) {
            // Detect if this a RENEWAL of a bulk certificate and if true, reject.   
            Long bulkId = this.jdbcRequestDao.getBulkIdForCertBy_Dn_Valid_NotExpired(csrRFC2253DN);
            if(bulkId != null){
                errors.reject("pkcs10.validation.request.renew.cert.bulkrenew", 
                        "Invalid Renew - Renewal maps to a bulk certificate and bulk renewals are not supported via the portal"
                                + " - please contact the helpdesk if you need this feature"); 
                return; 
            }
        }
    }

    private void validateCSR_NEW(Errors errors, PKCS10_RequestWrapper requestWrapper, 
            PKCS10CertificationRequest req){
        // Note, we do not check for VALID rows in the certificate table using 
        // RFC 1779 style DN (i.e. the 'does PKCS#10 request already exist with an RFC1779 DN' check). 
        // This is because all DNs in the cert table MUST be a RFC2253 style (i.e. 
        // comma-separated with no spaces - but host cert DNs may be pre-pended 
        // with emailAddress=<value>) 
        
        // Get the Subject DN string with following structure order: (CN, L, OU, O, C)  
        String csrRFC2253DN = CertUtil.getDbCanonicalDN(req.getSubject().toString());
        
        Profile profile = requestWrapper.getProfile(); 
        Csr_Types csr_type = requestWrapper.getCsr_type(); 
        if (Profile.UKHOST.equals(profile)) {
            if (CSR_Flags.Csr_Types.NEW.equals(csr_type)) {
                // For NEW host requests, we WANT to allow existing certs that MAY 
                // have the emailAddress= attribute to be re-requested. Therefore, 
                // we use "dn=canonicalDN" so that an existing cert will NOT be found
                // thus allowing the re-request to succeed.
            } else if (CSR_Flags.Csr_Types.RENEW.equals(csr_type)) {
                // For Host RENEW, if a cert exists in the DB with or without 
                // a leading emailAddress attribute, a certificate will be 
                // found using "dn like '%cannoncialDN'" so CSR will be accepted. 
                //namedParameters.put("dn", "%" + rfc2253DN);
                csrRFC2253DN = "%"+csrRFC2253DN; 
            } else {
                throw new IllegalStateException("Unsupported CSR Type");
            }
        } else if (Profile.UKPERSON.equals(profile)) {
        } else {
            throw new IllegalStateException("Unsupported CSR Profile");
        }
        
        if(this.jdbcCertificateDao.countByStatusIsVALIDNotExpired(csrRFC2253DN, 0) > 0){
           // if CSR=NEW, it is an error if a valid cert already exists 
            errors.reject("pkcs10.validation.request.already.exits.with.dn", 
                    "A Certificate with this DN already exists");
            return; 
        }
        
    }
    
    private void validateCommon(Errors errors, PKCS10_RequestWrapper requestWrapper,
            PublicKey pubkey, PKCS10CertificationRequest req) {

        // email is always needed
        if (!emailValidator.validate(requestWrapper.getEmail())) {
            errors.reject("pkcs10.validation.request.invalid.Email", 
            "Invalid Email");
            return; 
        }

        // Validate the csrPemString for a syntatcially correct PKCS10 
        csrValidator.validate(requestWrapper.getCsrPemString(), errors);
        if (errors.hasErrors()) {
            return;
        }

        // Check validity of RDN values, including the CN, email if given etc.
        dnValidator.validate(requestWrapper, errors);
        if (errors.hasErrors()) {
            return;
        }

        // Find if there is already a request in the 'request' table 
        // with the given public key and with a request.status that is not 'DELETED' 
        if (this.jdbcRequestDao.countByPublicKeyNotDeleted(
                ConvertUtil.getDBFormattedRSAPublicKey(pubkey)) > 0) {
            errors.reject("pkcs10.validation.request.already.exits.with.pubkey",
                    "Error - A request already exists with this public key");
            return;
        }

        // Get the Subject DN string with following structure order: (CN, L, OU, O, C)  
        String csrRFC2253DN = CertUtil.getDbCanonicalDN(req.getSubject().toString());
        if (Profile.UKPERSON.equals(requestWrapper.getProfile())) {
            // do nothing 
        } else if (Profile.UKHOST.equals(requestWrapper.getProfile())) {
            // See the ***IMPORTANT NOTE on canonical DN checking*** 
            // For host new or renewal, we want to match against existing requests 
            // that may have been previously stored with the 'emailAddress' attribute. 
            csrRFC2253DN = "%" + csrRFC2253DN;
        } else {
            throw new IllegalStateException("Unrecognized Profile");
        }

        // Find if there is already a request in the 'request' table that has 
        // the given DN with a status of 'NEW', 'APPROVED' or 'RENEW'. 
        // Also add spaces after each delim and re-check. This is necessary because 
        // OpenCA inserts DNs into the 'request.dn' column in the following 
        // format, i.e. RFC 1779 (not RFC 2253): "CN=some body, L=NGS, OU=Roaming, O=eScience, C=UK" 
        String spaced_csrRFC2253DN = csrRFC2253DN.replaceAll(",", ", ");
        log.debug("checking CSR DN against DB [" + csrRFC2253DN + "]");
        if ((this.jdbcRequestDao.countByDnWhereStatusNewApprovedRenew(csrRFC2253DN) > 0)
                || this.jdbcRequestDao.countByDnWhereStatusNewApprovedRenew(spaced_csrRFC2253DN) > 0) {
            errors.reject("pkcs10.validation.request.already.exits.with.dn",
                    "Error - A pending request already exists with this DN with state [NEW, APPROVED or RENEW]");
            return;
        }

        // We also need to check that CSRs with status NEW|APPROVED|RENEW 
        // don't exist in 'request' table with a reversed DN, e.g.  
        // "C=UK,O=eScience,L=Physics,OU=Imperial,CN=testhost00.grid.hep.ph.ic.ac.uk" 
        // There have been a few instances of these in the DB (I think csr signing  
        // may update the 'request.dn' column with a reversed dn if it was defined 
        // in reverese in the orignal PKCS10? - not sure).  
        String reversed_csrRFC2253DN = CertUtil.getReversedCommaSeparatedDN(csrRFC2253DN);
        String spaced_reversed_csrRFC2253DN = reversed_csrRFC2253DN.replaceAll(",", ", ");
        if ((this.jdbcRequestDao.countByDnWhereStatusNewApprovedRenew(reversed_csrRFC2253DN) > 0)
                || this.jdbcRequestDao.countByDnWhereStatusNewApprovedRenew(spaced_reversed_csrRFC2253DN) > 0) {
            errors.reject("pkcs10.validation.request.already.exits.with.dn",
                    "Error - A pending request already exists with this DN with state [NEW, APPROVED or RENEW]");
            return;
        }
    }

    @Autowired
    public void setJdbcRequestDao(JdbcRequestDao jdbcRequestDao) {
        this.jdbcRequestDao = jdbcRequestDao;
    }

    @Autowired
    public void setJdbcCertificateDao(JdbcCertificateDao jdbcCertificateDao) {
        this.jdbcCertificateDao = jdbcCertificateDao;
    }

    @Inject
    public void setMutableConfigParams(MutableConfigParams mutableConfigParams){
       this.mutableConfigParams = mutableConfigParams;  
    }

}
