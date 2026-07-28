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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;

import uk.ac.ngs.domain.CertificateRow;

@RunWith(MockitoJUnitRunner.class)
public class EmailServiceTests {

    @Mock
    private Sender mailSender;

    @Mock
    private SimpleMailMessage emailTemplate;

    @InjectMocks
    private EmailService emailService;

    private CertificateRow cert;

    @Before
    public void setUp() {
        cert = new CertificateRow();
        cert.setCert_key(123);
        cert.setEmail("user@test.com");
        cert.setCn("Test CN");
        cert.setDn("Test DN");

        emailService.setBasePortalUrl("https://portal.test");
        emailService.setEmailUserCertExpiryReminderTemplate("emailUserCertExpiryReminderTemplate.html");
    }

    @Test
    public void shouldSendEmailReminderSuccessfully() {

        int daysToExpire = 7;

        // when
        emailService.sendEmailReminderToUserOnCertExpiry(cert, daysToExpire);

        // then
        verify(mailSender).send(
                argThat(msg -> "user@test.com".equals(msg.getTo()[0]) &&
                        msg.getSubject().contains("7 days")),
                argThat(vars -> vars.get("certKey").equals(123L) &&
                        vars.get("daysToExpire").equals(7) &&
                        vars.get("cn").equals("Test CN") &&
                        vars.get("dn").equals("Test DN") &&
                        vars.get("basePortalUrl").equals("https://portal.test")),
                eq("emailUserCertExpiryReminderTemplate.html"));
    }

    @Test
    public void shouldHandleMailExceptionGracefully() {

        doThrow(new MailSendException("SMTP error"))
                .when(mailSender)
                .send(any(), any(), any());

        // no exception should be thrown
        assertDoesNotThrow(() -> emailService.sendEmailReminderToUserOnCertExpiry(cert, 7));

        verify(mailSender).send(any(), any(), any());
    }

}
