/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.ngs.service;

import java.io.IOException;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import uk.ac.ngs.common.MutableConfigParams;
import uk.ac.ngs.dao.JdbcCertificateDao;
import uk.ac.ngs.domain.CertificateRow;
import uk.ac.ngs.service.email.EmailService;
import uk.ac.ngs.validation.EmailValidator;

/**
 * Service class for transactional operations on the <pre>certificate</pre> table. 
 * 
 * @author Josh Hadley
 * @author Sam Worley 
 * @author David Meredith
 */
@Service
public class CertificateService {
    private static final Log log = LogFactory.getLog(CertificateService.class);
    private JdbcCertificateDao jdbcCertDao;
    private EmailService emailService;
    private MutableConfigParams mutableConfigParams; 
    private final static int flags = Pattern.CASE_INSENSITIVE | Pattern.MULTILINE;
    //pattern to match 'emailAddress' in cert's data field 
    private final static Pattern DATA_EMAIL_PATTERN = Pattern.compile("emailAddress\\s?=\\s?([^\\n]+)$", flags);
    private final static Pattern DATA_ROLE_PATTERN = Pattern.compile("ROLE\\s?=\\s?([^\\n]+)$", flags);
    private final EmailValidator emailValidator = new EmailValidator();
    
    /*@Transactional
    public int updateCertificateRow(CertificateRow certRow) {
        int rowsUpdated = jdbcCertDao.updateCertificateRow(certRow);
        return rowsUpdated;
    }*/
    
    /**
     * Update certificate email value in <code>certificate</code> DB table and 
     * send email notifications if configured to do email.
     * This function updates the email value in this table in two places:
     * <ul>
     *   <li>In the 'email' column</li>
     *   <li>In the 'data' column if there is an 'emailAddress=...' key-value pair</li>
     * </ul>
     * A number of pre-conditions are checked before an update is performed: 
     * <ul>
     *   <li>Submitted email value must be valid</li> 
     *   <li>Cert must not be expired</li> 
     *   <li>Cert must have a status of VALID</li>
     *   <li>Cert must be a HOST cert (contains a '.' char)</li>
     *   <li>Cert must not contain an email address (no '@' char)</li> 
     * </ul>
     * <p>
     * Updating the email address of a HOST certificate without renewing the cert  
     * is allowed because the email is not bound by the cert's digital signature 
     * (for host certificates the email value is only stored as separate field 
     * in the DB which can be updated without re-issuing a new cert). 
     * 
     * @param requesterDn current authenticated user's DN (user who is making the change)
     * @param cert_key PK of cert being updated
     * @param newEmail the new email value 
     * @return Errors Empty if there were no errors and email was updated ok 
     * @throws IOException
     */
    @Transactional
    public Errors updateCertificateRowEmail(String requesterDn, long cert_key, String newEmail) 
            throws IOException {
        return this.updateCertificateRowEmailHelper(requesterDn, cert_key, newEmail); 
    }


    private Errors updateCertificateRowEmailHelper(String requesterDn, long cert_key, String newEmail) 
            throws IOException {
        CertificateRow certRow = this.jdbcCertDao.findById(cert_key);
        String oldEmail = certRow.getEmail();

        // Validation 
        Errors errors = new MapBindingResult(new HashMap<String, String>(), "updateCertificateRowEmail");
        if (!emailValidator.validate(newEmail)) {
           errors.reject("validation.invalid.email.address", "Email update failed - invalid email address");  
        }
        if(!certRow.getCn().contains(".")){
            errors.reject("validation.invalid.email.nothostcert", "Email update failed - DN is not a host certificate"); 
        }
        if(certRow.getDn().contains("@")){
            errors.reject("validation.invalid.email.containsemail", "Email update failed - DN appears to contain an email address");
        }
        if(!"VALID".equals(certRow.getStatus()) ){
            errors.reject("validation.invalid.email.certnotVALID", "Email update failed - Certificate is not VALID");
        }
        if(this.isExpired(certRow)){
            errors.reject("validation.invalid.email.certexpired", "Email update failed - Certificate is expired"); 
        }
        // Also need to check the following (currently done in higher level controller):
        // <li>Current/authenticated user must be either the home RA or CAOP for the viewed cert</li>
        if(errors.hasErrors()){
           return errors;  
        }
        
        Matcher emailMatcher = DATA_EMAIL_PATTERN.matcher(certRow.getData());
        //if regex pattern is found, replace with new email address
        if(emailMatcher.find()) {
            String updatedData = emailMatcher.replaceAll("emailAddress=" + newEmail);
            certRow.setData(updatedData);
        }         
        // update email column with new email
        certRow.setEmail(newEmail);
        
        // Send emails to old and new email addresses (if configured to send email) 
        if (Boolean.parseBoolean(
                this.mutableConfigParams.getProperty("email.on.host.cert.email.update"))) {
            if(oldEmail == null) {
                this.emailService.sendEmailToNewEmailOnChange(certRow.getDn(), requesterDn, newEmail, cert_key);
            } else {
                this.emailService.sendEmailToOldAndNewOnEmailChange(certRow.getDn(), requesterDn, oldEmail, newEmail, cert_key);
            }
        }
        // finally update the certificate row and return numb of rows updated
        jdbcCertDao.updateCertificateRow(certRow);
        return errors; //empty 
    }

    /**
     * Compares current date with cert expiry date, returning a bool
     * @return true if cert has expired, else false
     */
    private boolean isExpired(CertificateRow certRow) {
        Date currDate = new Date(); 
        //TODO convert current date into UTC
        Date expireDate = certRow.getNotAfter(); // in UTC
        return currDate.after(expireDate);
    }
    
    /**
     * Update cert role value in <pre>certificate</pre> table.
     * This function updates the role value in two places in the DB:
     * <ul>
     *   <li>In the 'role' column</li>
     *   <li>In the 'data' column if there is an 'role=...' key-value pair</li>
     * </ul>
     * 
     * @param cert_key PK of cert being updated
     * @param new_cert_role the new role for the updated certificate
     * @return the number of rows updated (should always be one) 
     * @throws IOException
     */
    @Transactional
    public int updateCertificateRole(long cert_key, String new_cert_role) 
            throws IOException {
        return this.updateCertificateRoleHelper(cert_key, new_cert_role); 
    }
    
    private int updateCertificateRoleHelper(long cert_key, String new_cert_role) 
            throws IOException {
        CertificateRow certRow = this.jdbcCertDao.findById(cert_key);
        String newRole = new_cert_role;
        
        Matcher roleMatcher = DATA_ROLE_PATTERN.matcher(certRow.getData());
        //if regex pattern is found, replace with new role
        if(roleMatcher.find()) {
            String updatedData = roleMatcher.replaceAll("ROLE=" + newRole);
            certRow.setData(updatedData);
        }         
        // update role column with new role
        certRow.setRole(newRole);
                
        // Send email to owner of changed certificate (if configured to send email) 
        if (Boolean.parseBoolean(
                this.mutableConfigParams.getProperty("email.admins.on.role.change"))) {

                String[] admin = this.mutableConfigParams.getProperty("senior.caops").split(";");
                
                Map<JdbcCertificateDao.WHERE_PARAMS, String> whereParams = new 
                     EnumMap<JdbcCertificateDao.WHERE_PARAMS, String>(JdbcCertificateDao.WHERE_PARAMS.class);
                
                for(String dn: admin)
                {
                    whereParams.put(JdbcCertificateDao.WHERE_PARAMS.DN_LIKE, dn);
                    
                    CertificateRow ca = this.jdbcCertDao.findBy(whereParams, 1, 0).get(0);
                    
                    this.emailService.sendAdminEmailOnRoleChange(
                        certRow.getDn(), newRole, cert_key, ca.getEmail());
                }
        }
        // finally update the certificate row and return numb of rows updated
        return jdbcCertDao.updateCertificateRow(certRow);
    }
    
    @Inject 
    public void setJdbcCertificateDao(JdbcCertificateDao jdbcCertDao){
        this.jdbcCertDao = jdbcCertDao; 
    }
    
    @Inject
    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }
    
    @Inject
    public void setMutableConfigParams(MutableConfigParams mutableConfigParams) {
        this.mutableConfigParams = mutableConfigParams;
    }
}
