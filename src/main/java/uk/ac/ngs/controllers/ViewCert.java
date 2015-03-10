package uk.ac.ngs.controllers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.validation.Valid;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.ac.ngs.common.MutableConfigParams;
import uk.ac.ngs.dao.JdbcCertificateDao;
import uk.ac.ngs.domain.CertificateRow;
import uk.ac.ngs.forms.RevokeCertFormBean;
import uk.ac.ngs.security.SecurityContextService;
import uk.ac.ngs.service.CertUtil;
import uk.ac.ngs.service.CertificateService;
import uk.ac.ngs.service.ProcessRevokeService;
import uk.ac.ngs.service.ProcessRevokeService.ProcessRevokeResult;
import uk.ac.ngs.validation.EmailValidator;

/**
 * Controller for RA operators to view a certificate.
 *
 * @author David Meredith
 * @author Josh Hadley
 */
@Controller
@RequestMapping("/raop/viewcert")
public class ViewCert {

    private static final Log log = LogFactory.getLog(ViewCert.class);
    private SecurityContextService securityContextService;
    private JdbcCertificateDao certDao;
    private ProcessRevokeService processRevokeService; 
    private MutableConfigParams mutableConfigParams;
    private final EmailValidator emailValidator = new EmailValidator();
    private CertificateService certService;
    
    private final static Pattern DATA_PIN_PATTERN = Pattern.compile("PIN\\s?=\\s?(\\w+)$", Pattern.MULTILINE);
    private final static Pattern DATA_PROFILE_PATTERN = Pattern.compile("PROFILE\\s?=\\s?(\\w+)$", Pattern.MULTILINE);
    private final static Pattern DATA_LAD_PATTERN = Pattern.compile("LAST_ACTION_DATE\\s?=\\s?(.+)$", Pattern.MULTILINE);
    private final static Pattern DATA_CERT_PATTERN = Pattern.compile("-----BEGIN CERTIFICATE-----(.+?)-----END CERTIFICATE-----", Pattern.DOTALL);
    
    
    // In DOTALL mode, the expression . matches any character, including a line terminator. 
    // By default expressions do not match line terminators. 
    // The expresssion '.+?' means any character inc line terminators (DOTALL mode) one or more times. 
    
    
    @ModelAttribute
    public void populateDefaultModel(@RequestParam(required = false) Integer certId,
            ModelMap modelMap) {
        
        CertificateRow cert = new CertificateRow();
        modelMap.put("cert", cert);
        modelMap.put("canRevokeCert", false);
        modelMap.put("canEditEmail", false);
        modelMap.put("viewerCanFullRevoke", false);
        modelMap.put("viewerCanPromoteDemote", false); 
        modelMap.put("lastViewRefreshDate", new Date()); 
        modelMap.put("revokeCertFormBean", new RevokeCertFormBean());
        if (certId != null) {
            cert = this.certDao.findById(certId);
            modelMap.put("cert", cert);
            // Can current user edit email?
            if(cert.getCn().contains(".") && !(cert.getDn().contains("@")) && this.isCurrentUserCaOpOrHomeRaForViewCert(cert) && cert.getStatus().equals("VALID") && !(isExpired(cert))) {
                modelMap.put("canEditEmail", true);
            }
            // Set whether this cert can actually be revoked (VALID not expired) or not  
            modelMap.put("canRevokeCert", this.isCertRevokable(cert));
            // Set if view can do full revoke or request revoke (is RA who is 
            // viewing this cert a CAOP or a home RA for the current cert) 
            modelMap.put("viewerCanFullRevoke", this.isCurrentUserCaOpOrHomeRaForViewCert(cert));
            // Set if view can do full promote or demote certificates
            modelMap.put("viewerCanPromoteDemote", this.viewerCanPromoteDemote(cert));
       
            //Can current certificate being viewed be promoted?
            if("CA Operator".equals(cert.getRole()))
            {
                modelMap.put("canPromote", false); // can't promte a CA Op cert any higher!
            }
            else
            {
                modelMap.put("canPromote", true);
            }

            //Can cert be demoted?
            if("User".equals(cert.getRole()))
            {
                modelMap.put("canDemote", false);
            }
            else
            {
                modelMap.put("canDemote", true);
            }
        }
        
    }

    @RequestMapping(method = RequestMethod.GET)
    public void handleViewCertificate(@RequestParam(required = false) Integer certId,
            ModelMap modelMap) {
        
        if (certId == null) {
            modelMap.put("errorMessage", "Invalid certificate selection");
            return;
        }
        CertificateRow cert = (CertificateRow) modelMap.get("cert");

        // hex encoded certificate serial 
        modelMap.put("hexSerial", Long.toHexString(cert.getCert_key()));

        // Parse the data column 
        Matcher pinmatcher = DATA_PIN_PATTERN.matcher(cert.getData());
        if (pinmatcher.find()) {
            String pin = pinmatcher.group(1).toUpperCase();
            modelMap.put("pin", pin);
        }
        Matcher profilematcher = DATA_PROFILE_PATTERN.matcher(cert.getData());
        if (profilematcher.find()) {
            String profile = profilematcher.group(1);
            modelMap.put("profile", profile);
        }
        Matcher ladmatcher = DATA_LAD_PATTERN.matcher(cert.getData());
        if (ladmatcher.find()) {
            String lad = ladmatcher.group(1);
            modelMap.put("lastActionDate", lad);
        }
        Matcher certmatcher = DATA_CERT_PATTERN.matcher(cert.getData());
        if (certmatcher.find()) {
            String pemString = certmatcher.group();
            modelMap.put("certdata", pemString);
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                InputStream is = new ByteArrayInputStream(pemString.getBytes("UTF-8"));
                X509Certificate certObj = (X509Certificate) cf.generateCertificate(is);
                modelMap.put("certObj", certObj);
                //certObj.getNotAfter(); 
                //certObj.getNotBefore(); 
            } catch (CertificateException ex) {
                log.error(ex);
            } catch (UnsupportedEncodingException ex) {
                log.error(ex);
            }
        }
            
    }


    private boolean isCertRevokable(CertificateRow cert){
        if("VALID".equals(cert.getStatus()) && cert.getNotAfter().after(new Date())){
            return true; 
        } 
        return false; 
    }
        
    /**
     * TODO - this should be pushed to lower level service class. 
     * @param viewCert
     * @return 
     */
    private boolean isCurrentUserCaOpOrHomeRaForViewCert(CertificateRow viewCert){
         if (this.securityContextService.getCaUserDetails().getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_CAOP"))) {
            return true; 
        } else {
            String viewCertDN = viewCert.getDn();
            String raOpDN = this.securityContextService.getCaUserDetails().getCertificateRow().getDn();
            if (CertUtil.hasSameRA(raOpDN, viewCertDN)) {
                return true; 
            }
        }
         return false; 
    }
    
    /***
     * Check to see if a user can promote/demote certificates. SUPER CA-OP ONLY
     * @param viewCert certificate of the viewer
     * @return result of check
     */
    private boolean viewerCanPromoteDemote(CertificateRow cert){
        if (this.securityContextService.getCaUserDetails().getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_CAOP"))) {
            
            String profile = "default";
            Matcher profilematcher = DATA_PROFILE_PATTERN.matcher(cert.getData());
            
            if (profilematcher.find()) {
                profile = profilematcher.group(1);
            }
            
            //Check that certificate is a User, not a Host certificate
            if(profile.equals("UKPERSON"))
            {
                CertificateRow viewCert = this.securityContextService.getCaUserDetails().getCertificateRow();
                String dn = viewCert.getDn();
                log.debug(dn);
                
                try {
                    String[] superCA;
                    superCA = this.mutableConfigParams.getProperty("senior.caops").split(";");
                    log.debug("Super CAs: ");
                    for(String ca : superCA)
                    {
                        log.debug(ca);
                        if(ca.equals(dn))
                        {
                            return true;
                        }
                    }
                    
                    return false;
                }
                catch(IOException io)
                {
                    log.error(io);
                    return false;
                }
            }
            else
            {
                return false;
            }
        } else {
            return false;
        }       
    }

    /**
     * Handle POSTs to "/raop/viewcert/fullrevoke" to perform a full certificate 
     * revocation by either a HOME RA or a CAOP.
     * <p>
     * The view is always redirected and redirect attributes added as necessary; 
     * if the revoke is successful we redirect to a different 
     * page to view the CRR, if the revoke fails we need to re-display the 
     * current certificate by using the 'certId' URL/redirect attribute. 
     * 
     * @param revokeCertFormBean
     * @param result
     * @param redirectAttrs
     * @return "redirect:/raop/viewcert" on revocation failure, or 
     * "redirect:/raop/viewcert" on successful revocation. 
     */
    @RequestMapping(value="/fullrevoke", method=RequestMethod.POST)
    public String fullrevokeCertificate(@Valid RevokeCertFormBean revokeCertFormBean, BindingResult result,
        RedirectAttributes redirectAttrs) {
        long revoke_cert_key = revokeCertFormBean.getCert_key(); 
        if(result.hasErrors()){
            log.warn("binding and validation errors on fullrevokeCertificate");
            redirectAttrs.addFlashAttribute("errorMessage", "Revocation not submitted");
            StringBuilder bindError = new StringBuilder("");
            for (ObjectError error : result.getAllErrors()) {
                bindError.append(error.getDefaultMessage()).append(" ");
            }
            redirectAttrs.addFlashAttribute("formRevokeErrorMessage", bindError); 
            redirectAttrs.addAttribute("certId", revoke_cert_key);
            return "redirect:/raop/viewcert"; 
        } 

        ProcessRevokeResult revokeResult = processRevokeService.fullRevokeCertificate(
                revokeCertFormBean, securityContextService.getCaUserDetails().getCertificateRow());

        if (!revokeResult.getSuccess()) {
            redirectAttrs.addFlashAttribute("errorMessage", revokeResult.getErrors().getAllErrors().get(0).getDefaultMessage());
            redirectAttrs.addAttribute("certId", revoke_cert_key);
            return "redirect:/raop/viewcert";
        } else {
            redirectAttrs.addFlashAttribute("message", "Certificate SUSPENDED and an APPROVED CRR was created");
            redirectAttrs.addAttribute("requestId", revokeResult.getCrrId());
            return "redirect:/raop/viewcrr";
        }
    }


    /**
     * Handle POSTs to "/raop/viewcert/requestrevoke" to perform a certificate 
     * revocation request (i.e. any RA can request revoke, not just home RA). 
     * <p>
     * The view is always redirected and redirect attributes added as necessary; 
     * if the revoke is successful we redirect to a different 
     * page to view the CRR, if the revoke fails we need to re-display the 
     * current certificate by using the 'certId' URL/redirect attribute. 
     * 
     * @param revokeCertFormBean
     * @param result
     * @param redirectAttrs
     * @return "redirect:/raop/viewcert" on revocation failure, or 
     * "redirect:/raop/viewcert" on successful revocation. 
     * @throws java.io.IOException 
     */
    @RequestMapping(value = "/requestrevoke", method = RequestMethod.POST)
    public String requestrevokeCertificate(@Valid RevokeCertFormBean revokeCertFormBean, BindingResult result,
            RedirectAttributes redirectAttrs) throws IOException {
        long revoke_cert_key = revokeCertFormBean.getCert_key();
        if(result.hasErrors()){
            log.warn("binding and validation errors on requestRevoke");
            redirectAttrs.addFlashAttribute("errorMessage", "Revocation not submitted");
            StringBuilder bindError = new StringBuilder("");
            for (ObjectError error : result.getAllErrors()) {
                bindError.append(error.getDefaultMessage()).append(" ");
            }
            redirectAttrs.addFlashAttribute("formRevokeErrorMessage", bindError); 
            redirectAttrs.addAttribute("certId", revoke_cert_key);
            return "redirect:/raop/viewcert"; 
        } 

        ProcessRevokeResult revokeResult = processRevokeService.requestRevokeCertificate(
                revokeCertFormBean, securityContextService.getCaUserDetails().getCertificateRow()); 

        if (!revokeResult.getSuccess()) {
            redirectAttrs.addFlashAttribute("errorMessage", revokeResult.getErrors().getAllErrors().get(0).getDefaultMessage());
            redirectAttrs.addAttribute("certId", revoke_cert_key);
            return "redirect:/raop/viewcert";
        } else {
            redirectAttrs.addFlashAttribute("message", "Certificate SUSPENDED and a NEW CRR was created");
            redirectAttrs.addAttribute("requestId", revokeResult.getCrrId());
            return "redirect:/raop/viewcrr";
        }
        
    }
    
    /**
     * Called when RA attempts to update the viewed certificate's email address. 
     * A number of pre-conditions must be met before an update is performed: 
     * <ul>
     *   <li>Submitted email value must be valid</li> 
     *   <li>Current/authenticated user must be either the home RA or CAOP for the viewed cert</li>
     *   <li>Cert being viewed must be a host cert (contains a '.' char)</li>
     *   <li>Cert being viewed must not contain an email address (no '@' char)</li> 
     *   <li>Cert being viewed must be VALID</li> 
     *   <li>Cert being viewed must not be expired</li> 
     * </ul>
     * 
     * @param email new requested email address for cert
     * @param cert_key key of cert being updated
     * @param redirectAttrs
     * @return
     * @throws java.io.IOException
     */
    @RequestMapping(value = "/rachangemail", method = RequestMethod.POST)
    public String raChangeEmail(@RequestParam String email, @RequestParam long cert_key, 
            RedirectAttributes redirectAttrs) throws IOException {
        // cert being viewed
        CertificateRow viewCert = this.certDao.findById(cert_key);
        
        /*if (!emailValidator.validate(email)) {
            redirectAttrs.addFlashAttribute("emailUpdateFailMessage", "Cannot update email - invalid email");
        } else if (!isCurrentUserCaOpOrHomeRaForViewCert(viewCert)) {
            redirectAttrs.addFlashAttribute("emailUpdateFailMessage", "Cannot update email - must be CAOP or RAOP from same RA");
        } else if (!viewCert.getCn().contains(".")) {
            redirectAttrs.addFlashAttribute("emailUpdateFailMessage", "Cannot update email - DN is not a host certificate");
        } else if (viewCert.getDn().contains("@")) {
            redirectAttrs.addFlashAttribute("emailUpdateFailMessage", "Cannot update email - DN appears to contain an email address");
        } else if (!"VALID".equals(viewCert.getStatus())) {
            redirectAttrs.addFlashAttribute("emailUpdateFailMessage", "Cannot update email - Certificate is not VALID");
        } else if (isExpired(viewCert)) {
            redirectAttrs.addFlashAttribute("emailUpdateFailMessage", "Cannot update email - Certificate has expired");
        } else {*/
       
        if (!isCurrentUserCaOpOrHomeRaForViewCert(viewCert)) {
            redirectAttrs.addFlashAttribute("emailUpdateFailMessage", "Cannot update email - must be CAOP or RAOP from same RA");
        } else {
            CertificateRow currentRaUser = this.securityContextService.getCaUserDetails().getCertificateRow();
            log.info("RA changing cert email to (" + email + ") for certificate (" + cert_key + ") by RA (" + currentRaUser.getDn() + ")");
            Errors errors = this.certService.updateCertificateRowEmail(currentRaUser.getDn(), cert_key, email);
            if (errors.hasErrors()) {
                StringBuilder errorMsg = new StringBuilder();
                for (ObjectError e : errors.getAllErrors()) {
                    errorMsg.append(e.getDefaultMessage()).append("; ");
                }
                redirectAttrs.addFlashAttribute("emailUpdateFailMessage", errorMsg);
            } else {
                redirectAttrs.addFlashAttribute("emailUpdateOkMessage", "Email update OK");
            }
        }
        redirectAttrs.addAttribute("certId", cert_key);
        return "redirect:/raop/viewcert";
    }
    
    /**
     * Compares current date with cert expiry date, returning a bool
     * @return true if cert has expired, else false
     */
    private boolean isExpired(CertificateRow certRow) {
        Date currDate = new Date(); 
        //todo convert current date into UTC
        Date expireDate = certRow.getNotAfter();
        if(currDate.after(expireDate)) {
            return true;
        } 
        return false;
    }
    
    /**
     * Handle POSTs to "/raop/viewcert/changerole" to perform a promotion/demotion of 
     * certificate  by a CAOP.
     * <p>
     * The view is always redirected and redirect attributes added as necessary; 
     * if the change is successful we redirect to a different 
     * page to view the certificate, if the change fails we need to re-display the 
     * current certificate by using the 'certId' URL/redirect attribute. 
     * 
     * @param cert_key key of certificate being updated
     * @param operation whether it is promoting or demoting
     * @param redirectAttrs 
     * @return
     * @throws java.io.IOException
     */
    @RequestMapping(value="/changerole", method=RequestMethod.POST)
    public String changeRoleCertificate(@RequestParam long cert_key, @RequestParam String operation,
        RedirectAttributes redirectAttrs) throws IOException {
        long change_cert_key = cert_key;
        String op = operation;
        String cert_role;
        
        CertificateRow viewCert = this.certDao.findById(change_cert_key); 
        CertificateRow currentUser = this.securityContextService.getCaUserDetails().getCertificateRow();
        
        if(this.viewerCanPromoteDemote(currentUser))
        {
            //Get Role and either Promote or Demote the role.
            if("promote".equals(op))
            {
                if("User".equals(viewCert.getRole())) // User to RA-OP
                {
                    cert_role = "RA Operator";
                }
                else // RA-OP to CA-OP
                {
                    cert_role = "CA Operator";
                }
            }
            else
            {
                if("CA Operator".equals(viewCert.getRole())) // CA-OP to RA-OP
                {
                    cert_role = "RA Operator";
                }
                else // RA-OP to User
                {
                    cert_role = "User";
                }
            }


            log.info("role change from (" + viewCert.getRole() + ") to ("+ cert_role +")for certificate (" + change_cert_key + ") by (" + currentUser.getDn() + ")");

            this.certService.updateCertificateRole(change_cert_key, cert_role);
            redirectAttrs.addFlashAttribute("roleChangeOkMessage", "Role Change OK");
            redirectAttrs.addAttribute("certId", change_cert_key);
            return "redirect:/raop/viewcert";
        }
        else {
            
            redirectAttrs.addFlashAttribute("roleChangeFailMessage", "Role Change FAIL - Viewer does not have correct permissions");
            redirectAttrs.addAttribute("certId", change_cert_key);
            return "redirect:/raop/viewcert";
        }
    }
    
    @Inject
    public void setJdbcCertificateDao(JdbcCertificateDao dao) {
        this.certDao = dao;
    }

    @Inject
    public void setSecurityContextService(SecurityContextService securityContextService) {
        this.securityContextService = securityContextService;
    }
    
    @Inject
    public void setMutableConfigParams(MutableConfigParams mutableConfigParams){
        this.mutableConfigParams = mutableConfigParams;  
    }

    /**
     * @param processRevokeService the processRevokeService to set
     */
    @Inject
    public void setProcessRevokeService(ProcessRevokeService processRevokeService) {
        this.processRevokeService = processRevokeService;
    }
    
    @Inject
    public void setCertificateService(CertificateService certService) {
        this.certService = certService;
    }
}
