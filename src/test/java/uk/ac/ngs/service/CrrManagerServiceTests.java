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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import uk.ac.ngs.dao.JdbcCertificateDao;
import uk.ac.ngs.dao.JdbcCrrDao;
import uk.ac.ngs.domain.CertificateRow;
import uk.ac.ngs.domain.CrrRow;
import uk.ac.ngs.service.CrrManagerService.CRR_STATUS;

@RunWith(MockitoJUnitRunner.class)
public class CrrManagerServiceTests {
    @Mock
    private JdbcCrrDao jdbcCrrDao;
    @Mock
    private JdbcCertificateDao jdbcCertDao;
    @Mock
    X509Certificate x509;
    @Mock
    X500Principal x500Principal;

    private LocalDate localDate = LocalDate.now().plusDays(1);
    private Date datePlusOneDay = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

    private CrrManagerService crrManagerService;

    @Before
    public void setUp() {
        crrManagerService = new CrrManagerService();
        crrManagerService.setJdbcCrrDao(jdbcCrrDao);
        crrManagerService.setJdbcCertificateDao(jdbcCertDao);
    }

    private CertificateRow getCertRow() {
        CertificateRow certRow = new CertificateRow();
        certRow.setCn("VALID.CN");
        certRow.setDn("VALID.DN");
        certRow.setStatus("VALID");
        certRow.setData("ROLE=oldRole");
        certRow.setNotAfter(datePlusOneDay);

        return certRow;
    }

    private CrrRow getCrrRow() {
        CrrRow crrRow = new CrrRow();
        crrRow.setCert_key(1313L);

        return crrRow;
    }

    @Test
    // @WithMockUser(username = "user", roles = {"ROLE_RAOP", "ROLE_CAOP"})
    public void testRevokeCertificateThrowsExceptionForInvalidCertKeyToRevoke() {
        long cert_key_toRevoke = -1;
        long raop_cert_key = 1;
        String reason = "Valid Reason";
        CRR_STATUS crrStatus = CRR_STATUS.APPROVED;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            crrManagerService.revokeCertificate(cert_key_toRevoke, raop_cert_key, reason, crrStatus);
        });
        assertEquals("Invalid cert_key_toRevoke", exception.getMessage());
    }

    @Test
    public void testRevokeCertificateThrowsExceptionForInvalidRaopCertKey() {
        long cert_key_toRevoke = 1;
        long raop_cert_key = -1;
        String reason = "Valid Reason";
        CRR_STATUS crrStatus = CRR_STATUS.APPROVED;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            crrManagerService.revokeCertificate(cert_key_toRevoke, raop_cert_key, reason, crrStatus);
        });
        assertEquals("Invalid raop_cert_key", exception.getMessage());
    }

    @Test
    public void testRevokeCertificateThrowsExceptionForInvalidReason() {
        long cert_key_toRevoke = 1;
        long raop_cert_key = 1;
        String reason = null;
        CRR_STATUS crrStatus = CRR_STATUS.APPROVED;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            crrManagerService.revokeCertificate(cert_key_toRevoke, raop_cert_key, reason, crrStatus);
        });
        assertEquals("Invalid reason", exception.getMessage());
    }

    @Test
    public void testRevokeCertificateThrowsExceptionForInvalidStatus() {
        long cert_key_toRevoke = 1;
        long raop_cert_key = 1;
        String reason = "Valid Reason";
        CRR_STATUS crrStatus = CRR_STATUS.DELETED;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            crrManagerService.revokeCertificate(cert_key_toRevoke, raop_cert_key, reason, crrStatus);
        });
        assertEquals("Invalid new CRR status requested - required NEW or APPROVED", exception.getMessage());
    }

    @Test
    public void testRevokeCertificateThrowsExceptionForNoCertRow() {
        long cert_key_toRevoke = 1;
        long raop_cert_key = 1;
        String reason = "Valid Reason";
        CRR_STATUS crrStatus = CRR_STATUS.APPROVED;

        when(jdbcCertDao.findById(cert_key_toRevoke)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            crrManagerService.revokeCertificate(cert_key_toRevoke, raop_cert_key, reason, crrStatus);
        });
        assertEquals("Invalid certificate row - no row found with id [" + cert_key_toRevoke + "]",
                exception.getMessage());
    }

    @Test
    public void testRevokeCertificateThrowsExceptionForUpdatingMultipleCertificate() {
        long cert_key_toRevoke = 1;
        long raop_cert_key = 1;
        String reason = "Valid Reason";
        CRR_STATUS crrStatus = CRR_STATUS.APPROVED;
        CertificateRow certRow = getCertRow();

        when(jdbcCertDao.findById(cert_key_toRevoke)).thenReturn(certRow);
        when(jdbcCertDao.updateCertificateRow(certRow)).thenReturn(2);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            crrManagerService.revokeCertificate(cert_key_toRevoke, raop_cert_key, reason, crrStatus);
        });
        assertEquals("Multiple certificate rows attempted for udpate", exception.getMessage());
    }

    @Test
    public void testRevokeCertificateThrowsExceptionForUpdatingMultipleCrrRows()
            throws CertificateException, UnsupportedEncodingException {
        long cert_key_toRevoke = 1;
        long raop_cert_key = 1;
        String reason = "Valid Reason";
        CRR_STATUS crrStatus = CRR_STATUS.APPROVED;
        CertificateRow certRow = getCertRow();

        when(jdbcCertDao.findById(cert_key_toRevoke)).thenReturn(certRow);
        when(jdbcCertDao.updateCertificateRow(certRow)).thenReturn(1);
        when(jdbcCertDao.getX509CertificateFromData(certRow)).thenReturn(x509);
        when(x509.getIssuerX500Principal()).thenReturn(x500Principal);
        when(jdbcCrrDao.insertCrrRow(any())).thenReturn(2);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            crrManagerService.revokeCertificate(cert_key_toRevoke, raop_cert_key, reason, crrStatus);
        });
        assertEquals("Multiple crr rows attempted for udpate", exception.getMessage());
    }

    @Test
    public void testRevokeCertificateReturnCrrKeyOnSucess() throws CertificateException, UnsupportedEncodingException {
        long cert_key_toRevoke = 1;
        long raop_cert_key = 1;
        String reason = "Valid Reason";
        CRR_STATUS crrStatus = CRR_STATUS.APPROVED;
        CertificateRow certRow = getCertRow();
        Long nextCrrKey = 1212L;

        when(jdbcCertDao.findById(cert_key_toRevoke)).thenReturn(certRow);
        when(jdbcCertDao.updateCertificateRow(certRow)).thenReturn(1);
        when(jdbcCertDao.getX509CertificateFromData(certRow)).thenReturn(x509);
        // when(x509.getIssuerDN()).thenReturn(any());
        when(x509.getIssuerX500Principal()).thenReturn(x500Principal);
        when(jdbcCrrDao.getNextCrr_key()).thenReturn(nextCrrKey);
        when(jdbcCrrDao.insertCrrRow(any())).thenReturn(1);

        Long crrKey = crrManagerService.revokeCertificate(cert_key_toRevoke, raop_cert_key, reason, crrStatus);
        assertEquals(nextCrrKey, crrKey);
        assertEquals("SUSPENDED", certRow.getStatus());
    }

    @Test
    public void testSelfRevokeCertificateThrowsExceptionForInvalidCertKeyToRevoke() {
        long cert_key_toRevoke = -1;
        String reason = "Valid Reason";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            crrManagerService.selfRevokeCertificate(cert_key_toRevoke, reason);
        });
        assertEquals("Invalid cert_key_toRevoke", exception.getMessage());
    }

    @Test
    public void testSelfRevokeCertificateThrowsExceptionForInvalidReason() {
        long cert_key_toRevoke = 1;
        String reason = null;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            crrManagerService.selfRevokeCertificate(cert_key_toRevoke, reason);
        });
        assertEquals("Invalid reason", exception.getMessage());
    }

    @Test
    public void testSelfRevokeCertificateThrowsExceptionForNoCertRow() {
        long cert_key_toRevoke = 1;
        String reason = "Valid Reason";

        when(jdbcCertDao.findById(cert_key_toRevoke)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            crrManagerService.selfRevokeCertificate(cert_key_toRevoke, reason);
        });
        assertEquals("Invalid certificate row - no row found with id [" + cert_key_toRevoke + "]",
                exception.getMessage());
    }

    @Test
    public void testSelfRevokeCertificateThrowsExceptionForUpdatingMultipleCertificate() {
        long cert_key_toRevoke = 1;
        String reason = "Valid Reason";
        CertificateRow certRow = getCertRow();

        when(jdbcCertDao.findById(cert_key_toRevoke)).thenReturn(certRow);
        when(jdbcCertDao.updateCertificateRow(certRow)).thenReturn(2);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            crrManagerService.selfRevokeCertificate(cert_key_toRevoke, reason);
        });
        assertEquals("Multiple certificate rows attempted for udpate", exception.getMessage());
    }

    @Test
    public void testSelfRevokeCertificateThrowsExceptionForUpdatingMultipleCrrRows()
            throws CertificateException, UnsupportedEncodingException {
        long cert_key_toRevoke = 1;
        String reason = "Valid Reason";
        CertificateRow certRow = getCertRow();

        when(jdbcCertDao.findById(cert_key_toRevoke)).thenReturn(certRow);
        when(jdbcCertDao.updateCertificateRow(certRow)).thenReturn(1);
        when(jdbcCertDao.getX509CertificateFromData(certRow)).thenReturn(x509);
        when(x509.getIssuerX500Principal()).thenReturn(x500Principal);
        when(jdbcCrrDao.insertCrrRow(any())).thenReturn(2);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            crrManagerService.selfRevokeCertificate(cert_key_toRevoke, reason);
        });
        assertEquals("Multiple crr rows attempted for udpate", exception.getMessage());
    }

    @Test
    public void testSelfRevokeCertificateReturnCrrKeyOnSucess()
            throws CertificateException, UnsupportedEncodingException {
        long cert_key_toRevoke = 1;
        String reason = "Valid Reason";
        CertificateRow certRow = getCertRow();
        Long nextCrrKey = 1212L;

        when(jdbcCertDao.findById(cert_key_toRevoke)).thenReturn(certRow);
        when(jdbcCertDao.updateCertificateRow(certRow)).thenReturn(1);
        when(jdbcCertDao.getX509CertificateFromData(certRow)).thenReturn(x509);
        when(x509.getIssuerX500Principal()).thenReturn(x500Principal);
        when(jdbcCrrDao.getNextCrr_key()).thenReturn(nextCrrKey);
        when(jdbcCrrDao.insertCrrRow(any())).thenReturn(1);

        Long crrKey = crrManagerService.selfRevokeCertificate(cert_key_toRevoke, reason);
        assertEquals(nextCrrKey, crrKey);
        assertEquals("SUSPENDED", certRow.getStatus());
    }

    @Test
    public void testDeleteRevocationRequestThrowsExceptionForInvalidCrrKeyToDelete() {
        long crr_key_toDelete = -1;
        long raopId = 1;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            crrManagerService.deleteRevocationRequest(crr_key_toDelete, raopId);
        });
        assertEquals("Invalid crr_key_toDelete", exception.getMessage());
    }

    @Test
    public void testDeleteRevocationRequestThrowsExceptionForInvalidRaopId() {
        long crr_key_toDelete = 1;
        long raopId = -1;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            crrManagerService.deleteRevocationRequest(crr_key_toDelete, raopId);
        });
        assertEquals("Invalid raopId", exception.getMessage());
    }

    @Test
    public void testDeleteRevocationRequestThrowsExceptionForNoCrrRow() {
        long crr_key_toDelete = 1;
        long raopId = 1;

        when(jdbcCrrDao.findById(crr_key_toDelete)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            crrManagerService.deleteRevocationRequest(crr_key_toDelete, raopId);
        });
        assertEquals("Invalid crrRow - no row found with id " + crr_key_toDelete, exception.getMessage());
    }

    @Test
    public void testDeleteRevocationRequestThrowsExceptionForNoCertRow() {
        long crr_key_toDelete = 1;
        long raopId = 1;
        CrrRow crrRow = getCrrRow();

        when(jdbcCrrDao.findById(crr_key_toDelete)).thenReturn(crrRow);
        when(jdbcCertDao.findById(crrRow.getCert_key())).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            crrManagerService.deleteRevocationRequest(crr_key_toDelete, raopId);
        });
        assertEquals("Invalid certRow - no row found with id " + crrRow.getCert_key(), exception.getMessage());
    }

    @Test
    public void testDeleteRevocationRequestThrowsExceptionForInvalidCrrStatus() {
        long crr_key_toDelete = 1;
        long raopId = 1;
        CrrRow crrRow = getCrrRow();
        crrRow.setStatus(CRR_STATUS.ARCHIVED.toString());

        when(jdbcCrrDao.findById(crr_key_toDelete)).thenReturn(crrRow);
        when(jdbcCertDao.findById(crrRow.getCert_key())).thenReturn(getCertRow());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            crrManagerService.deleteRevocationRequest(crr_key_toDelete, raopId);
        });
        assertEquals("Invalid crrRow - status is not NEW or APPROVED: " + crr_key_toDelete, exception.getMessage());
    }

    @Test
    public void testDeleteRevocationRequestThrowsExceptionForMultipleCrrRows() {
        long crr_key_toDelete = 1;
        long raopId = 1;
        CrrRow crrRow = getCrrRow();
        crrRow.setStatus(CRR_STATUS.NEW.toString());

        when(jdbcCrrDao.findById(crr_key_toDelete)).thenReturn(crrRow);
        when(jdbcCrrDao.updateCrrRow(crrRow)).thenReturn(2);
        when(jdbcCertDao.findById(crrRow.getCert_key())).thenReturn(getCertRow());
        when(jdbcCrrDao.updateDataCol_StatusRaop(crrRow.getData(), CRR_STATUS.DELETED.toString(), raopId))
                .thenReturn(anyString());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            crrManagerService.deleteRevocationRequest(crr_key_toDelete, raopId);
        });
        assertEquals("Multiple crr rows attempted for udpate", exception.getMessage());
    }

    @Test
    public void testDeleteRevocationRequestThrowsExceptionForMultipleCertificateRows() {
        long crr_key_toDelete = 1;
        long raopId = 1;
        CrrRow crrRow = getCrrRow();
        crrRow.setStatus(CRR_STATUS.NEW.toString());
        CertificateRow certRow = getCertRow();

        when(jdbcCrrDao.findById(crr_key_toDelete)).thenReturn(crrRow);
        when(jdbcCrrDao.updateCrrRow(crrRow)).thenReturn(1);
        when(jdbcCertDao.findById(crrRow.getCert_key())).thenReturn(certRow);
        when(jdbcCertDao.updateCertificateRow(certRow)).thenReturn(2);
        when(jdbcCrrDao.updateDataCol_StatusRaop(crrRow.getData(), CRR_STATUS.DELETED.toString(), raopId))
                .thenReturn(anyString());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            crrManagerService.deleteRevocationRequest(crr_key_toDelete, raopId);
        });
        assertEquals("Multiple certificate rows attempted for udpate", exception.getMessage());
    }

    @Test
    public void testDeleteRevocationRequestUpdateStatusOnSuccess() {
        long crr_key_toDelete = 1;
        long raopId = 1;
        CrrRow crrRow = getCrrRow();
        crrRow.setStatus(CRR_STATUS.NEW.toString());
        CertificateRow certRow = getCertRow();

        when(jdbcCertDao.findById(crrRow.getCert_key())).thenReturn(certRow);
        when(jdbcCertDao.updateCertificateRow(certRow)).thenReturn(1);
        when(jdbcCrrDao.findById(crr_key_toDelete)).thenReturn(crrRow);
        when(jdbcCrrDao.updateCrrRow(crrRow)).thenReturn(1);
        when(jdbcCrrDao.updateDataCol_StatusRaop(crrRow.getData(), CRR_STATUS.DELETED.toString(), raopId))
                .thenReturn(anyString());

        crrManagerService.deleteRevocationRequest(crr_key_toDelete, raopId);

        assertEquals(CRR_STATUS.DELETED.toString(), crrRow.getStatus());
        assertEquals("VALID", certRow.getStatus());
        verify(jdbcCertDao, times(1)).updateCertificateRow(certRow);
    }

    @Test
    public void testApproveRevocationRequestThrowsExceptionForInvalidCrrKeyToDelete() {
        long crr_key_toApprove = -1;
        long raopId = 1;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            crrManagerService.approveRevocationRequest(crr_key_toApprove, raopId);
        });
        assertEquals("Invalid crr_key_toApprove", exception.getMessage());
    }

    @Test
    public void testApproveRevocationRequestThrowsExceptionForInvalidRaopId() {
        long crr_key_toApprove = 1;
        long raopId = -1;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            crrManagerService.approveRevocationRequest(crr_key_toApprove, raopId);
        });
        assertEquals("Invalid raopId", exception.getMessage());
    }

    @Test
    public void testApproveRevocationRequestThrowsExceptionForNoCrrRow() {
        long crr_key_toApprove = 1;
        long raopId = 1;

        when(jdbcCrrDao.findById(crr_key_toApprove)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            crrManagerService.approveRevocationRequest(crr_key_toApprove, raopId);
        });
        assertEquals("Invalid crrRow - no row found with id " + crr_key_toApprove, exception.getMessage());
    }

    @Test
    public void testApproveRevocationRequestThrowsExceptionForInvalidCrrStatus() {
        long crr_key_toApprove = 1;
        long raopId = 1;
        CrrRow crrRow = getCrrRow();
        crrRow.setStatus(CRR_STATUS.ARCHIVED.toString());

        when(jdbcCrrDao.findById(crr_key_toApprove)).thenReturn(crrRow);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            crrManagerService.approveRevocationRequest(crr_key_toApprove, raopId);
        });
        assertEquals("Invalid crrRow - expected crr status is NEW or APPROVED but was: " + crrRow.getStatus(),
                exception.getMessage());
    }

    @Test
    public void testApproveRevocationRequestThrowsExceptionForMultipleCrrRows() {
        long crr_key_toApprove = 1;
        long raopId = 1;
        CrrRow crrRow = getCrrRow();
        crrRow.setStatus(CRR_STATUS.APPROVED.toString());

        when(jdbcCrrDao.findById(crr_key_toApprove)).thenReturn(crrRow);
        when(jdbcCrrDao.updateCrrRow(crrRow)).thenReturn(2);
        when(jdbcCrrDao.updateDataCol_StatusRaop(crrRow.getData(), CRR_STATUS.APPROVED.toString(), raopId))
                .thenReturn(anyString());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            crrManagerService.approveRevocationRequest(crr_key_toApprove, raopId);
        });
        assertEquals("Multiple crr rows attempted for udpate", exception.getMessage());
    }

    @Test
    public void testApproveRevocationRequestUpdateStatusOnSuccess() {
        long crr_key_toApprove = 1;
        long raopId = 1;
        CrrRow crrRow = getCrrRow();
        crrRow.setStatus(CRR_STATUS.APPROVED.toString());

        when(jdbcCrrDao.findById(crr_key_toApprove)).thenReturn(crrRow);
        when(jdbcCrrDao.updateCrrRow(crrRow)).thenReturn(1);
        when(jdbcCrrDao.updateDataCol_StatusRaop(crrRow.getData(), CRR_STATUS.APPROVED.toString(), raopId))
                .thenReturn(anyString());

        crrManagerService.approveRevocationRequest(crr_key_toApprove, raopId);

        assertEquals(CRR_STATUS.APPROVED.toString(), crrRow.getStatus());
        verify(jdbcCrrDao, times(1)).updateCrrRow(crrRow);
    }
}
