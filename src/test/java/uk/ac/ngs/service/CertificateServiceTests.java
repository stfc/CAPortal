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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.validation.Errors;

import uk.ac.ngs.common.MutableConfigParams;
import uk.ac.ngs.dao.JdbcCertificateDao;
import uk.ac.ngs.domain.CertificateRow;
import uk.ac.ngs.service.email.EmailService;

@RunWith(MockitoJUnitRunner.class)
public class CertificateServiceTests {
    @Mock
    JdbcCertificateDao jdbcCertDao;
    @Mock
    MutableConfigParams mutableConfigParams;
    @Mock
    EmailService emailService;

    private String requesterDn = "CN=TestUser";
    private long cert_key = 1000L;
    private String newEmail = "newEmail@example.com";
    private LocalDate plusOneDayLocalDate = LocalDate.now().plusDays(1);
    private Date datePlusOneDay = Date.from(plusOneDayLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    private LocalDate oldLocalDate = LocalDate.now().minusDays(10);
    private Date oldDate = Date.from(oldLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

    private CertificateService certificateService;

    @Before
    public void setUp() {
        certificateService = new CertificateService();
        certificateService.setJdbcCertificateDao(jdbcCertDao);
        certificateService.setMutableConfigParams(mutableConfigParams);
        certificateService.setEmailService(emailService);
    }

    @Test
    public void testUpdateCertificateRole() throws IOException {
        String newCertRole = "newRole";
        CertificateRow certRow = new CertificateRow();
        certRow.setCn("VALID.CN");
        certRow.setDn("VALID.DN");
        certRow.setStatus("VALID");
        certRow.setData("ROLE=oldRole");
        certRow.setNotAfter(datePlusOneDay);

        when(jdbcCertDao.findById(cert_key)).thenReturn(certRow);
        when(jdbcCertDao.findBy(anyMap(), anyInt(), anyInt())).thenReturn(new ArrayList<>(Arrays.asList(certRow)));
        when(mutableConfigParams.getProperty("email.admins.on.role.change")).thenReturn("TRUE");
        when(mutableConfigParams.getProperty("senior.caops")).thenReturn("adminDn_1;adminDn_2");

        certificateService.updateCertificateRole(cert_key, newCertRole);

        assertEquals("ROLE=" + newCertRole, certRow.getData());
        assertEquals(newCertRole, certRow.getRole());
        verify(emailService, times(2)).sendAdminEmailOnRoleChange(
                certRow.getDn(), newCertRole, cert_key, certRow.getEmail());
        verify(jdbcCertDao, times(1)).updateCertificateRow(certRow);
    }

    @Test
    public void testUpdateCertificateRowEmailGererateErrorsForInvalidEmail() throws IOException {
        CertificateRow certRow = new CertificateRow();
        certRow.setCn("VALID.CN");
        certRow.setDn("VALID.DN");
        certRow.setStatus("VALID");
        certRow.setNotAfter(datePlusOneDay);

        when(jdbcCertDao.findById(cert_key)).thenReturn(certRow);

        Errors errors = certificateService.updateCertificateRowEmail(requesterDn, cert_key, "INVALID_EMAIL");

        assertEquals(1, errors.getErrorCount());
        assertEquals("Email update failed - invalid email address", errors.getAllErrors().get(0).getDefaultMessage());
    }

    @Test
    public void testUpdateCertificateRowEmailGererateErrorsForInvalidCertRow() throws IOException {
        CertificateRow certRow = new CertificateRow();
        certRow.setCn("INVALID_CN");
        certRow.setDn(newEmail);
        certRow.setStatus("INVALID");
        certRow.setNotAfter(oldDate);

        when(jdbcCertDao.findById(cert_key)).thenReturn(certRow);

        Errors errors = certificateService.updateCertificateRowEmail(requesterDn, cert_key, newEmail);

        assertEquals(4, errors.getErrorCount());
        assertEquals("Email update failed - DN is not a host certificate",
                errors.getAllErrors().get(0).getDefaultMessage());
        assertEquals("Email update failed - DN appears to contain an email address",
                errors.getAllErrors().get(1).getDefaultMessage());
        assertEquals("Email update failed - Certificate is not VALID",
                errors.getAllErrors().get(2).getDefaultMessage());
        assertEquals("Email update failed - Certificate is expired",
                errors.getAllErrors().get(3).getDefaultMessage());
    }

    @Test
    public void testUpdateCertificateRowEmailUpdateCertRowData() throws IOException {
        CertificateRow certRow = new CertificateRow();
        certRow.setCn("VALID.CN");
        certRow.setDn("VALID.DN");
        certRow.setStatus("VALID");
        certRow.setData("emailAddress=oldEmail@example.com");
        certRow.setNotAfter(datePlusOneDay);

        when(jdbcCertDao.findById(cert_key)).thenReturn(certRow);

        Errors errors = certificateService.updateCertificateRowEmail(requesterDn, cert_key, newEmail);

        verify(jdbcCertDao, times(1)).updateCertificateRow(certRow);
        assertEquals(0, errors.getErrorCount());
        assertEquals("emailAddress=" + newEmail, certRow.getData());
    }

    @Test
    public void testUpdateCertificateRowEmailSendEmailToNewEmailIfOldEmailNotPresent() throws IOException {
        CertificateRow certRow = new CertificateRow();
        certRow.setCn("VALID.CN");
        certRow.setDn("VALID.DN");
        certRow.setStatus("VALID");
        certRow.setData("emailAddress=oldEmail@example.com");
        certRow.setNotAfter(datePlusOneDay);

        when(jdbcCertDao.findById(cert_key)).thenReturn(certRow);
        when(mutableConfigParams.getProperty("email.on.host.cert.email.update")).thenReturn("TRUE");

        Errors errors = certificateService.updateCertificateRowEmail(requesterDn, cert_key, newEmail);

        verify(jdbcCertDao, times(1)).updateCertificateRow(certRow);
        assertEquals(0, errors.getErrorCount());
        verify(emailService, times(1))
                .sendEmailToNewEmailOnChange(certRow.getDn(), requesterDn, newEmail, cert_key);
    }

    @Test
    public void testUpdateCertificateRowEmailSendEmailToBothIfOldEmailPresent() throws IOException {
        String oldEmail = "oldEmail@example.com";
        CertificateRow certRow = new CertificateRow();
        certRow.setCn("VALID.CN");
        certRow.setDn("VALID.DN");
        certRow.setEmail(oldEmail);
        certRow.setStatus("VALID");
        certRow.setData("emailAddress=" + oldEmail);
        certRow.setNotAfter(datePlusOneDay);

        when(jdbcCertDao.findById(cert_key)).thenReturn(certRow);
        when(mutableConfigParams.getProperty("email.on.host.cert.email.update")).thenReturn("TRUE");

        Errors errors = certificateService.updateCertificateRowEmail(requesterDn, cert_key, newEmail);

        verify(jdbcCertDao, times(1)).updateCertificateRow(certRow);
        assertEquals(0, errors.getErrorCount());
        verify(emailService, times(1))
                .sendEmailToOldAndNewOnEmailChange(certRow.getDn(), requesterDn, oldEmail, newEmail, cert_key);
    }
}
