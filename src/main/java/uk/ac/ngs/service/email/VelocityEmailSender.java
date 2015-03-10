/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.ngs.service.email;

import java.util.Map;
import javax.mail.internet.MimeMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Component;
import org.springframework.ui.velocity.VelocityEngineUtils;

/**
 * Sends an email using a named Velocity template for the message body.
 *
 * @author David Meredith
 */
@Component
public class VelocityEmailSender implements Sender {

    private static final Log log = LogFactory.getLog(VelocityEmailSender.class);

    private final VelocityEngine velocityEngine;
    private final JavaMailSender mailSender;

    /**
     * Constructor
     *
     * @param velocityEngine
     * @param mailSender
     */
    @Autowired
    public VelocityEmailSender(VelocityEngine velocityEngine,
            JavaMailSender mailSender) {
        this.velocityEngine = velocityEngine;
        this.mailSender = mailSender;
    }

    @Override
    public void send(final SimpleMailMessage msg,
            final Map<String, Object> hTemplateVariables, final String templateFileName) {
        
        MimeMessagePreparator preparator = new MimeMessagePreparator() {
            @Override
            public void prepare(MimeMessage mimeMessage) throws Exception {
                MimeMessageHelper message = new MimeMessageHelper(mimeMessage);
                message.setTo(msg.getTo());
                message.setFrom(msg.getFrom());
                message.setSubject(msg.getSubject());
                String body = VelocityEngineUtils.mergeTemplateIntoString(
                        velocityEngine, templateFileName, hTemplateVariables);

               //logger.info("body={}", body);
                message.setText(body, true);
            }
        };

        mailSender.send(preparator);
    }

}
