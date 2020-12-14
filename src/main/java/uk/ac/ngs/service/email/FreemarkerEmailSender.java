package uk.ac.ngs.service.email;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;
import java.util.Map;

import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

@Component
public class FreemarkerEmailSender implements Sender {

    private static final Log log = LogFactory.getLog(FreemarkerEmailSender.class);
    private final FreeMarkerConfigurer freemarkerConfig;
    private final JavaMailSender mailSender;

    @Autowired
    public FreemarkerEmailSender(FreeMarkerConfigurer freemarkerConfig, JavaMailSender mailSender) {
        this.freemarkerConfig = freemarkerConfig;
        this.mailSender = mailSender;
    }

    @Override
    public void send(SimpleMailMessage msg, Map<String, Object> hTemplateVariables, String templateFileName) {
        MimeMessagePreparator preparator = new MimeMessagePreparator() {
            @Override
            public void prepare(MimeMessage mimeMessage) throws Exception {
                MimeMessageHelper message = new MimeMessageHelper(mimeMessage);
                message.setTo(msg.getTo());
                message.setFrom(msg.getFrom());
                message.setSubject(msg.getSubject());
                String body = FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfig.getConfiguration().getTemplate(templateFileName), hTemplateVariables);
                log.info("body=" + body);
                message.setText(body, true);
            }
        };

        mailSender.send(preparator);
    }
}
