package uk.ac.ngs;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import uk.ac.ngs.common.MutableConfigParams;
import uk.ac.ngs.security.SecurityContextService;
import uk.ac.ngs.service.email.EmailService;
import uk.ac.ngs.validation.CsrRequestDbValidator;
import uk.ac.ngs.validation.CsrRequestValidationConfigParams;
import uk.ac.ngs.validation.PKCS10SubjectDNValidator;
import uk.ac.ngs.validation.PKCS10Validator;

@Configuration
@EnableWebMvc
@PropertySource("classpath:/project.properties")
public class WebConfig implements WebMvcConfigurer {

    @Value("${supported.pkcs10.country.oid}")
    private String countryOID;
    @Value("${supported.pkcs10.orgname.oid}")
    private String orgName;
    @Value("${supported.pkcs10.min.modulus}")
    private Integer minModulus;
    @Value("${supported.pkcs10.min.exponent}")
    private Integer minExponent;
    @Value("${mutable.config.params.full.path}")
    private String mutableConfigParams;
    @Value("${base.portal.url}")
    private String basePortalUrl;
    @Value("${email.host}")
    private String emailHost;
    @Value("${email.from}")
    private String emailFrom;

    @Value("${jdbc.driverClassName}")
    private String jdbcDriverClassName;
    @Value("${jdbc.url}")
    private String jdbcUrl;
    @Value("${jdbc.username}")
    private String jdbcUsername;
    @Value("${jdbc.password}")
    private String jdbcPassword;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/resources/**")
                .addResourceLocations("/resources/");
    }

    @Bean
    public MutableConfigParams mutableConfigParams() {
        return new MutableConfigParams(mutableConfigParams);
    }

    @Bean
    public FreeMarkerConfigurer freemarkerConfig() {
        FreeMarkerConfigurer freeMarkerConfigurer = new FreeMarkerConfigurer();
        freeMarkerConfigurer.setTemplateLoaderPath("/WEB-INF/freemarker/email/");
        return freeMarkerConfigurer;
    }

    @Bean
    public CsrRequestValidationConfigParams csrRequestValidationParams() {
        CsrRequestValidationConfigParams p = new CsrRequestValidationConfigParams(countryOID, orgName);
        p.setMinModulus(minModulus);
        p.setMinExponent(minExponent);
        return p;
    }

    @Bean
    public PKCS10SubjectDNValidator dnValidator() {
        return new PKCS10SubjectDNValidator(csrRequestValidationParams());
    }

    @Bean
    public PKCS10Validator p10Validator() {
        return new PKCS10Validator(csrRequestValidationParams());
    }

    @Bean
    public CsrRequestDbValidator csrRequestDbValidator() {
        return new CsrRequestDbValidator(dnValidator(), p10Validator());
    }

    @Bean
    public EmailService emailService() {
        EmailService emailService = new EmailService();
        emailService.setEmailTemplate(baseEmailTemplate());
        emailService.setEmailRaRenewTemplate("emailRaRenewTemplate.html");
        emailService.setEmailRaNewHostTemplate("emailRaNewHostTemplate.html");
        emailService.setEmailRaNewUserTemplate("emailRaNewUserTemplate.html");
        emailService.setEmailRaRevokeTemplate("emailRaRevokeTemplate.html");
        emailService.setEmailUserNewUserCertTemplate("emailUserNewUserCertTemplate.html");
        emailService.setEmailUserNewHostCertTemplate("emailUserNewHostCertTemplate.html");
        emailService.setEmailAdminsOnErrorTemplate("emailAdminsOnErrorTemplate.html");
        emailService.setEmailRaEmailChangeTemplate("emailRaEmailChangeTemplate.html");
        emailService.setBasePortalUrl(basePortalUrl);
        return emailService;
    }

    @Bean
    public SimpleMailMessage baseEmailTemplate() {
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setFrom(emailFrom);
        simpleMailMessage.setSubject("UK CA Action Required");
        return simpleMailMessage;
    }

    @Bean
    public JavaMailSenderImpl mailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(emailHost);
        return mailSender;
    }

    @Bean
    public BasicDataSource dataSource() {
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName(jdbcDriverClassName);
        ds.setUrl(jdbcUrl);
        ds.setUsername(jdbcUsername);
        ds.setPassword(jdbcPassword);
        return ds;
    }

    @Bean
    public SecurityContextService securityContextService() {
        return new SecurityContextService();
    }

    @Bean
    public InternalResourceViewResolver internalResourceViewResolver() {
        InternalResourceViewResolver i = new InternalResourceViewResolver();
        i.setPrefix("/WEB-INF/views/");
        i.setSuffix(".jsp");
        return i;
    }
}
