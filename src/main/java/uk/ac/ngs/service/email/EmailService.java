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
package uk.ac.ngs.service.email;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import uk.ac.ngs.common.CertUtil;
import uk.ac.ngs.domain.CSR_Flags;

import javax.inject.Inject;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Send CA specific e-mails such as RA and User NEW/RENEW notifications.
 *
 * @author David Meredith
 */
public class EmailService {

    private static final Log log = LogFactory.getLog(EmailService.class);
    private Sender mailSender;
    private SimpleMailMessage emailTemplate;
    private String emailRaRenewTemplate;
    private String emailRaNewHostTemplate;
    private String emailRaNewUserTemplate;
    private String emailRaRevokeTemplate;
    private String emailRaEmailChangeTemplate;
    private String emailRaNewEmailChangeTemplate;
    private String emailUserNewUserCertTemplate;
    private String emailUserNewHostCertTemplate;
    private String emailAdminsRoleChangeTemplate;
    private String emailAdminsOnErrorTemplate;
    private String emailAdminsOnNewRaTemplate;
    private String emailRaHostRenewTemplate;
    private String emailAdminsOnRaopRoleRequestTemplate;
    private String emailRaOnRaopRoleRequestTemplate;
    private String emailUserOnRaopRoleRequestTemplate;
    private String emailOnRaopRoleRequestApprovalTemplate;
    private String emailOnRaopRoleRequestRejectionTemplate;
    private String emailOnRoleChangeToUserTemplate;

    private String basePortalUrl;

    public EmailService() {
    }

    /**
     * Email the user of their new host cert request.
     *
     * @param requestorCN    The CN/name of the requestor.
     * @param requestedDN    the requested cert DN
     * @param recipientEmail
     * @param raCNs
     * @param raEmails
     */
    public void sendRequestorOnNewHost(String requestorCN, String requestedDN,
                                       String recipientEmail, String raCNs, String raEmails) {
        SimpleMailMessage msg = new SimpleMailMessage(this.emailTemplate);
        msg.setTo(recipientEmail);
        Map<String, Object> vars = new HashMap<>();

        vars.put("clientCN", requestorCN);
        vars.put("requestedDN", requestedDN);
        vars.put("raCNs", raCNs);
        vars.put("raEmails", raEmails);

        try {
            this.mailSender.send(msg, vars, this.emailUserNewHostCertTemplate);
        } catch (MailException ex) {
            log.error("MailSender " + ex.getMessage());
        }
    }

    /**
     * Email the user of their CSR request.
     *
     * @param dn             DN of the new CSR
     * @param recipientEmail
     * @param raCNs
     * @param raEmails
     */
    public void sendRequestorOnNewUser(String dn,
                                       String recipientEmail, String raCNs, String raEmails) {
        SimpleMailMessage msg = new SimpleMailMessage(this.emailTemplate);
        msg.setTo(recipientEmail);
        Map<String, Object> vars = new HashMap<>();

        vars.put("cn", CertUtil.extractDnAttribute(dn, CertUtil.DNAttributeType.CN));
        vars.put("dn", dn);
        vars.put("raCNs", raCNs);
        vars.put("raEmails", raEmails);

        try {
            this.mailSender.send(msg, vars, this.emailUserNewUserCertTemplate);
        } catch (MailException ex) {
            log.error("MailSender " + ex.getMessage());
        }
    }


    /**
     * Email the portal admins on occurence of an error.
     *
     * @param adminEmails
     * @param cause       The causal exception (if known)
     * @param url         The url that caused the exception (if known)
     */
    public void sendAdminsOnError(Set<String> adminEmails, Exception cause, String url) {
        StringBuilder emailDebug = new StringBuilder("Emailing following Admins on Error [");
        for (String adminEmail : adminEmails) {
            emailDebug.append(adminEmail);
            emailDebug.append("; ");
            // Create a thread safe "copy" of the template message and customize it 
            SimpleMailMessage msg = new SimpleMailMessage(this.emailTemplate);
            msg.setTo(adminEmail);
            //msg.setFrom("support@grid-support.ac.uk");
            msg.setSubject("UK CA Portal error notification");
            Map<String, Object> vars = new HashMap<>();
            vars.put("basePortalUrl", basePortalUrl);
            if (cause != null) {
                vars.put("exception", cause);
                vars.put("message", cause.getMessage());
                StringWriter sw = new StringWriter();
                cause.printStackTrace(new PrintWriter(sw));
                vars.put("stacktrace", sw.toString());
            } else {
                vars.put("exception", "unknown");
                vars.put("message", "unknown");
                vars.put("stacktrace", "unknown");
            }
            if (url != null) {
                vars.put("url", url);
            }
            try {
                this.mailSender.send(msg, vars, this.emailAdminsOnErrorTemplate);
            } catch (MailException ex) {
                log.error("MailSender " + ex.getMessage());
            }
        }
        emailDebug.append("]");
        log.warn(emailDebug);
    }

    /**
     * Email the RAs of the pending revocation request.
     *
     * @param requestedRevokeDN
     * @param raEmails
     * @param crrId
     */
    public void sendRaEmailOnRevoke(String requestedRevokeDN, Set<String> raEmails,
                                    long crrId) {
        StringBuilder emailDebug = new StringBuilder("Emailing following RAs on REVOKE [");
        for (String raEmail : raEmails) {
            emailDebug.append(raEmail);
            emailDebug.append("; ");
            // Create a thread safe "copy" of the template message and customize it 
            SimpleMailMessage msg = new SimpleMailMessage(this.emailTemplate);
            msg.setTo(raEmail);
            Map<String, Object> vars = new HashMap<>();
            vars.put("param1", requestedRevokeDN);
            vars.put("basePortalUrl", basePortalUrl);
            vars.put("crrId", crrId);
            try {
                this.mailSender.send(msg, vars, this.emailRaRevokeTemplate);
            } catch (MailException ex) {
                log.error("MailSender " + ex.getMessage());
            }
        }
        emailDebug.append("]");
        log.debug(emailDebug);
    }

    /**
     * Email the RAs of the pending CSR NEW request.
     *
     * @param profile
     * @param requestedDN
     * @param raEmails
     * @param req_key
     */
    public void sendRaEmailOnCsrNew(CSR_Flags.Profile profile, String requestedDN,
                                    Set<String> raEmails, long req_key) {
        StringBuilder emailDebug = new StringBuilder("Emailing following RAs on NEW [");

        for (String raEmail : raEmails) {
            emailDebug.append(raEmail);
            emailDebug.append("; ");
            // Create a thread safe "copy" of the template message and customize it 
            SimpleMailMessage msg = new SimpleMailMessage(this.emailTemplate);
            msg.setTo(raEmail);
            Map<String, Object> vars = new HashMap<>();
            vars.put("param1", requestedDN);
            vars.put("basePortalUrl", basePortalUrl);
            vars.put("req_key", req_key);
            try {
                if (CSR_Flags.Profile.UKHOST.equals(profile)) {
                    this.mailSender.send(msg, vars, this.emailRaNewHostTemplate);
                } else if (CSR_Flags.Profile.UKPERSON.equals(profile)) {
                    this.mailSender.send(msg, vars, this.emailRaNewUserTemplate);
                }
            } catch (MailException ex) {
                log.error("MailSender " + ex.getMessage());
            }
        }
        emailDebug.append("]");
        log.debug(emailDebug);
    }

    /**
     * Email the RAs of the pending CSR RENEW request.
     *
     * @param requestedRenewDN
     * @param raEmails
     * @param req_key
     * @param emailUpdated
     * @param b
     */
    public void sendRaEmailOnCsrRenew(String requestedRenewDN, Set<String> raEmails,
                                      long req_key, boolean emailUpdated, boolean isHostCert) {
        StringBuilder emailDebug = new StringBuilder("Emailing following RAs on RENEW [");
        for (String raEmail : raEmails) {
            emailDebug.append(raEmail);
            emailDebug.append("; ");
            // Create a thread safe "copy" of the template message and customize it 
            SimpleMailMessage msg = new SimpleMailMessage(this.emailTemplate);
            msg.setTo(raEmail);
            Map<String, Object> vars = new HashMap<>();
            vars.put("param1", requestedRenewDN);
            vars.put("basePortalUrl", basePortalUrl);
            vars.put("req_key", req_key);
            if (emailUpdated) {
                vars.put("emailUpdatedTxt", "Important - this renewal requests "
                        + "a change of email - please ensure email is valid when approving.");
            } else {
                vars.put("emailUpdatedTxt", "This renewal does not include a change of email address.");
            }
            try {
                if (isHostCert) {
                    this.mailSender.send(msg, vars, this.emailRaHostRenewTemplate);
                } else {
                    this.mailSender.send(msg, vars, this.emailRaRenewTemplate);
                }
            } catch (MailException ex) {
                log.error(ex.getMessage());
            }
        }
        emailDebug.append("]");
        log.debug(emailDebug);
    }

    /**
     * Email both the old and new email addresses on change of email.
     *
     * @param certDn      DN of the certificate that is being updated
     * @param requesterDn DN of the user making the change request
     * @param oldEmail    Previous email address
     * @param newEmail    New email address
     * @param cert_key    PK of cert being updated
     */
    public void sendEmailToOldAndNewOnEmailChange(String certDn, String requesterDn,
                                                  String oldEmail, String newEmail, long cert_key) {
        log.debug("sendRaEmailOnEmailChange method");
        StringBuilder emailDebug = new StringBuilder("Emailing old and new email addresses [");
        //sends email to both old and new email addresses
        Set<String> oldNewEmails = new HashSet<>(); // use set so duplicates aren't added
        oldNewEmails.add(oldEmail);
        oldNewEmails.add(newEmail);

        for (String email : oldNewEmails) {
            emailDebug.append(email);
            emailDebug.append("; ");
            // Create a thread safe "copy" of the template message and customize it 
            SimpleMailMessage msg = new SimpleMailMessage(this.emailTemplate);
            msg.setTo(email);
            msg.setSubject("UK CA Email Updated");
            Map<String, Object> vars = new HashMap<>();
            vars.put("cert_dn", certDn);
            vars.put("requester_dn", requesterDn);
            vars.put("basePortalUrl", basePortalUrl);
            vars.put("req_key", cert_key);
            vars.put("old_email", oldEmail);
            vars.put("new_email", newEmail);
            try {
                this.mailSender.send(msg, vars, this.emailRaEmailChangeTemplate);
            } catch (MailException ex) {
                log.error("MailSender " + ex.getMessage());
            }
        }
        emailDebug.append("]");
        log.debug(emailDebug);
    }

    /**
     * Email new address only - called if email address had not previously been set.
     *
     * @param certDn      DN of the certificate that is being updated
     * @param requesterDn DN of the user making the change request
     * @param newEmail    New email address
     * @param cert_key    PK of cert being updated
     */
    public void sendEmailToNewEmailOnChange(String certDn, String requesterDn,
                                            String newEmail, long cert_key) {
        log.debug("sendEmailToNewEmailOnChange method");
        StringBuilder emailDebug = new StringBuilder("Emailing new email address [");
        emailDebug.append(newEmail);
        // Create a thread safe "copy" of the template message and customize it 
        SimpleMailMessage msg = new SimpleMailMessage(this.emailTemplate);
        msg.setTo(newEmail);
        msg.setSubject("UK CA Email Updated");
        Map<String, Object> vars = new HashMap<>();
        vars.put("cert_dn", certDn);
        vars.put("requester_dn", requesterDn);
        vars.put("basePortalUrl", basePortalUrl);
        vars.put("req_key", cert_key);
        vars.put("new_email", newEmail);
        try {
            this.mailSender.send(msg, vars, this.emailRaNewEmailChangeTemplate);
        } catch (MailException ex) {
            log.error("MailSender " + ex.getMessage());
        }

        emailDebug.append("]");
        log.debug(emailDebug);
    }

    /**
     * Email Update on Role Change - Called when User Role is Changed
     *
     * @param certDn    DN of the certificate that is being updated
     * @param role      New Role of Certificate
     * @param cert_key  Key of the updated Certificate
     * @param userEmail E-mail address of local RA Manager / CA Manager
     */
    public void sendAdminEmailOnRoleChange(String certDn, String role, long cert_key, String userEmail) {
        log.debug("sendAdminEmailOnRoleChange method");
        StringBuilder emailDebug = new StringBuilder("Emailing new role [");
        emailDebug.append(role);
        // Create a thread safe "copy" of the template message and customize it 
        SimpleMailMessage msg = new SimpleMailMessage(this.emailTemplate);
        msg.setTo(userEmail);
        msg.setSubject("UK CA Role Updated");
        Map<String, Object> vars = new HashMap<>();
        vars.put("cert_dn", certDn);
        vars.put("basePortalUrl", basePortalUrl);
        vars.put("req_key", cert_key);
        vars.put("new_role", role);
        try {
            this.mailSender.send(msg, vars, this.emailAdminsRoleChangeTemplate);
        } catch (MailException ex) {
            log.error("MailSender " + ex.getMessage());
        }

        emailDebug.append("]");
        log.debug(emailDebug);
    }

     /**
     * Email the CAOP on user to RAOP role change request.
     *
     * @param requesterCN    The CN/name of the requester.
     * @param targetDN    the target cert DN
     * @param recipientEmail
     */
    public void sendAdminsOnRaopRoleRequest(String caopCN, String requesterCN, String targetDN, String recipientEmail) {
        SimpleMailMessage msg = new SimpleMailMessage(this.emailTemplate);
        msg.setTo(recipientEmail);
        Map<String, Object> vars = new HashMap<>();

        vars.put("caopCN", caopCN);
        vars.put("requesterCN", requesterCN);
        vars.put("targetDN", targetDN);
        vars.put("basePortalUrl", basePortalUrl);

        try {
            this.mailSender.send(msg, vars, this.emailAdminsOnRaopRoleRequestTemplate);
        } catch (MailException ex) {
            log.error("MailSender " + ex.getMessage());
        }
    }

     /**
     * Email the RAOP on user to RAOP role change request.
     *
     * @param requesterCN    The CN/name of the requester.
     * @param targetDN    the target cert DN
     * @param recipientEmail
     */
    public void sendRaOnRaopRoleRequest(String requesterCN, String targetDN, String recipientEmail) {
        SimpleMailMessage msg = new SimpleMailMessage(this.emailTemplate);
        msg.setTo(recipientEmail);
        Map<String, Object> vars = new HashMap<>();

        vars.put("requesterCN", requesterCN);
        vars.put("targetDN", targetDN);

        try {
            this.mailSender.send(msg, vars, this.emailRaOnRaopRoleRequestTemplate);
        } catch (MailException ex) {
            log.error("MailSender " + ex.getMessage());
        }
    }

     /**
     * Email the user on user to RAOP role change request.
     *
     * @param requesterCN    The CN/name of the requester.
     * @param targetCN    the target cert CN
     * @param recipientEmail
     */
    public void sendUserOnRaopRoleRequest(String requesterCN, String targetCN, String recipientEmail) {
        SimpleMailMessage msg = new SimpleMailMessage(this.emailTemplate);
        msg.setTo(recipientEmail);
        Map<String, Object> vars = new HashMap<>();

        vars.put("requesterCN", requesterCN);
        vars.put("targetCN", targetCN);

        try {
            this.mailSender.send(msg, vars, this.emailUserOnRaopRoleRequestTemplate);
        } catch (MailException ex) {
            log.error("MailSender " + ex.getMessage());
        }
    }

     /**
     * Email on user to RAOP role change request approval.
     *
     * @param recipient    The CN/name of the recipient
     * @param actorCN    The CN/name of the actor CAOP
     * @param targetDN    The target cert DN
     * @param recipientEmail
     */
     public void sendEmailOnRaopRoleRequestApproval(String recipient, String actorCN, String targetDN,
             String recipientEmail) {
         SimpleMailMessage msg = new SimpleMailMessage(this.emailTemplate);
         msg.setTo(recipientEmail);
         Map<String, Object> vars = new HashMap<>();

         vars.put("recipient", recipient);
         vars.put("actorCN", actorCN);
         vars.put("targetDN", targetDN);

         try {
             this.mailSender.send(msg, vars, this.emailOnRaopRoleRequestApprovalTemplate);
         } catch (MailException ex) {
             log.error("MailSender " + ex.getMessage());
         }
     }

    /**
     * Email on user to RAOP role change request rejection.
     *
     * @param recipient    The CN/name of the recipient
     * @param actorCN    The CN/name of the actor CAOP
     * @param targetDN    the target cert DN
     * @param recipientEmail
     */
    public void sendEmailOnRaopRoleRequestRejection(String recipient, String actorCN, String targetDN,
            String recipientEmail) {
        SimpleMailMessage msg = new SimpleMailMessage(this.emailTemplate);
        msg.setTo(recipientEmail);
        Map<String, Object> vars = new HashMap<>();

        vars.put("recipient", recipient);
        vars.put("actorCN", actorCN);
        vars.put("targetDN", targetDN);

        try {
            this.mailSender.send(msg, vars, this.emailOnRaopRoleRequestRejectionTemplate);
        } catch (MailException ex) {
            log.error("MailSender " + ex.getMessage());
        }
    }

    /**
     * Email on RAOP to user role change.
     *
     * @param recipient    The CN/name of the recipient.
     * @param actorCN    The CN/name of the actor RAOP.
     * @param targetDN    the target cert DN
     * @param recipientEmail
     */
    public void sendEmailOnRoleChangeToUser(String recipient, String actorCN, String targetDN, String recipientEmail) {
        SimpleMailMessage msg = new SimpleMailMessage(this.emailTemplate);
        msg.setTo(recipientEmail);
        Map<String, Object> vars = new HashMap<>();

        vars.put("recipient", recipient);
        vars.put("actorCN", actorCN);
        vars.put("targetDN", targetDN);

        try {
            this.mailSender.send(msg, vars, this.emailOnRoleChangeToUserTemplate);
        } catch (MailException ex) {
            log.error("MailSender " + ex.getMessage());
        }
    }

    @Inject
    public void setSender(Sender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * @param emailTemplate the raRenewEmailTemplate to set
     */
    public void setEmailTemplate(SimpleMailMessage emailTemplate) {
        this.emailTemplate = emailTemplate;
    }

    /**
     * @param emailRaHostRenewTemplate the emailRaRenewTemplate to set
     */
    public void setEmailRaHostRenewTemplate(String emailRaHostRenewTemplate) {
        this.emailRaHostRenewTemplate = emailRaHostRenewTemplate;
    }

    /**
     * @param emailRaRenewTemplate the emailRaRenewTemplate to set
     */
    public void setEmailRaRenewTemplate(String emailRaRenewTemplate) {
        this.emailRaRenewTemplate = emailRaRenewTemplate;
    }

    /**
     * @param emailRaNewHostTemplate the emailRaNewHostTemplate to set
     */
    public void setEmailRaNewHostTemplate(String emailRaNewHostTemplate) {
        this.emailRaNewHostTemplate = emailRaNewHostTemplate;
    }

    /**
     * @param emailRaNewUserTemplate the emailRaNewUserTemplate to set
     */
    public void setEmailRaNewUserTemplate(String emailRaNewUserTemplate) {
        this.emailRaNewUserTemplate = emailRaNewUserTemplate;
    }

    /**
     * @param emailRaRevokeTemplate the emailRaRevokeTemplate to set
     */
    public void setEmailRaRevokeTemplate(String emailRaRevokeTemplate) {
        this.emailRaRevokeTemplate = emailRaRevokeTemplate;
    }

    /**
     * @param emailRaEmailChange the emailRaEmailChange
     */
    public void setEmailRaEmailChangeTemplate(String emailRaEmailChange) {
        this.emailRaEmailChangeTemplate = emailRaEmailChange;
    }

    /**
     * @param emailRaNewEmailChange the emailRaNewEmailChange
     */
    public void setEmailRaNewEmailChangeTemplate(String emailRaNewEmailChange) {
        this.emailRaNewEmailChangeTemplate = emailRaNewEmailChange;
    }

    /**
     * @param url the url that will be used to construct links back to the portal
     */
    public void setBasePortalUrl(String url) {
        this.basePortalUrl = url;
    }

    /**
     * @param emailUserNewUserCertTemplate the emailUserNewUserCertTemplate to set
     */
    public void setEmailUserNewUserCertTemplate(String emailUserNewUserCertTemplate) {
        this.emailUserNewUserCertTemplate = emailUserNewUserCertTemplate;
    }

    /**
     * @param emailUserNewHostCertTemplate the emailUserNewHostCertTemplate to set
     */
    public void setEmailUserNewHostCertTemplate(String emailUserNewHostCertTemplate) {
        this.emailUserNewHostCertTemplate = emailUserNewHostCertTemplate;
    }

    /**
     * @param emailUserRoleChangeTemplate the emailUserRoleChangeTemplate to set
     */

    /**
     * @param emailAdminsOnErrorTemplate the emailAdminsOnErrorTemplate to set
     */
    public void setEmailAdminsOnErrorTemplate(String emailAdminsOnErrorTemplate) {
        this.emailAdminsOnErrorTemplate = emailAdminsOnErrorTemplate;
    }

    /**
     * @param emailAdminsOnRaopRoleRequestTemplate the emailAdminsOnRaopRoleRequestTemplate to set
     */
    public void setEmailAdminsOnRaopRoleRequestTemplate(String emailAdminsOnRaopRoleRequestTemplate) {
        this.emailAdminsOnRaopRoleRequestTemplate = emailAdminsOnRaopRoleRequestTemplate;
    }

    /**
     * @param emailRaOnRaopRoleRequestTemplate the emailRaOnRaopRoleRequestTemplate to set
     */
    public void setEmailRaOnRaopRoleRequestTemplate(String emailRaOnRaopRoleRequestTemplate) {
        this.emailRaOnRaopRoleRequestTemplate = emailRaOnRaopRoleRequestTemplate;
    }

    /**
     * @param emailUserOnRaopRoleRequestTemplate the emailUserOnRaopRoleRequestTemplate to set
     */
    public void setEmailUserOnRaopRoleRequestTemplate(String emailUserOnRaopRoleRequestTemplate) {
        this.emailUserOnRaopRoleRequestTemplate = emailUserOnRaopRoleRequestTemplate;
    }

    /**
     * @param emailOnRaopRoleRequestApprovalTemplate the emailOnRaopRoleRequestApprovalTemplate to set
     */
    public void setEmailOnRaopRoleRequestApprovalTemplate(String emailOnRaopRoleRequestApprovalTemplate) {
        this.emailOnRaopRoleRequestApprovalTemplate = emailOnRaopRoleRequestApprovalTemplate;
    }

    /**
     * @param emailOnRaopRoleRequestRejectionTemplate the emailOnRaopRoleRequestRejectionTemplate to set
     */
    public void setEmailOnRaopRoleRequestRejectionTemplate(String emailOnRaopRoleRequestRejectionTemplate) {
        this.emailOnRaopRoleRequestRejectionTemplate = emailOnRaopRoleRequestRejectionTemplate;
    }

    /**
     * @param emailOnRoleChangeToUserTemplate the emailOnRoleChangeToUserTemplate to set
     */
    public void setEmailOnRoleChangeToUserTemplate(String emailOnRoleChangeToUserTemplate) {
        this.emailOnRoleChangeToUserTemplate = emailOnRoleChangeToUserTemplate;
    }

}
