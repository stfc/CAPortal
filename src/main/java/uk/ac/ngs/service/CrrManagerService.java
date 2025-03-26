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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ngs.dao.JdbcCertificateDao;
import uk.ac.ngs.dao.JdbcCrrDao;
import uk.ac.ngs.domain.CertificateRow;
import uk.ac.ngs.domain.CrrRow;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Service class for managing CRRs including deletion and approval.link
 * Public service methods are transactional and are limited to specific roles.
 *
 * @author David Meredith
 */
@Service
public class CrrManagerService {
    public enum CRR_STATUS {

        APPROVED, NEW, DELETED, ARCHIVED
    }

    private JdbcCrrDao jdbcCrrDao;
    private JdbcCertificateDao jdbcCertDao;

    // Date will be of the form:  Tue Apr 23 13:47:13 2013 UTC 
    private final DateFormat crrDateFormat = new SimpleDateFormat("EEE MMM d hh:mm:ss yyyy z");


    public CrrManagerService() {
        this.crrDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }


    /**
     * Revoke the certificate identified by the first PK using the second cert
     * PK/serial to identify the revoker. After successful completion, the
     * certificate status will be 'SUSPENDED' and a new 'crr' db row will be
     * inserted with the specified reason and crrStatus.
     * <p>
     * Given crr status must be APPROVED or NEW.
     * Note, there are no checks to ensure the certificate to be revoked is VALID and not-expired.
     * There are no checks to determine if the raop is home ra (or not).
     *
     * @param cert_key_toRevoke PK of the certificate to revoke
     * @param raop_cert_key     PK of the revoking certificate
     * @param reason
     * @param crrStatus         Status string for the new crr row either NEW or APPROVED
     * @return The ID/PK of the newly inserted db 'crr' row.
     */
    @RolesAllowed({"ROLE_RAOP", "ROLE_CAOP"})
    @Transactional
    public long revokeCertificate(long cert_key_toRevoke, long raop_cert_key,
                                  String reason, CRR_STATUS crrStatus) {
        // throw early  
        if (cert_key_toRevoke <= 0) {
            throw new IllegalArgumentException("Invalid cert_key_toRevoke");
        }
        if (raop_cert_key <= 0) {
            throw new IllegalArgumentException("Invalid raop_cert_key");
        }
        if (reason == null) {
            throw new IllegalArgumentException("Invalid reason");
        }

        if (!CRR_STATUS.APPROVED.toString().equals(crrStatus.toString()) &&
                !CRR_STATUS.NEW.toString().equals(crrStatus.toString())) {
            throw new IllegalArgumentException("Invalid new CRR status requested - required NEW or APPROVED");
        }

        // search for an existing CRR for this cert? 
        CertificateRow certRow = this.jdbcCertDao.findById(cert_key_toRevoke);
        if (certRow == null) {
            throw new RuntimeException("Invalid certificate row - no row found with id [" + cert_key_toRevoke + "]");
        }

        // update the cert row to suspended 
        certRow.setStatus("SUSPENDED");
        // therefore also update the LAST_ACTION_DATE and RAOP values in the data column 
        certRow.setData(this.jdbcCertDao.updateDataCol_LastActionDateRaop(certRow.getData(), raop_cert_key));
        if (this.jdbcCertDao.updateCertificateRow(certRow) != 1) {
            throw new RuntimeException("Multiple certificate rows attempted for udpate");
        }

        // create a new crr row 
        CrrRow crrRow = this.buildCrrRow(certRow, reason, crrStatus, raop_cert_key);
        if (this.jdbcCrrDao.insertCrrRow(crrRow) != 1) {
            throw new RuntimeException("Multiple crr rows attempted for udpate");
        }
        return crrRow.getCrr_key();
    }

    /**
     * Revoke the certificate identified by the given certificate PK/serial.
     * Since this is a self-revocation, the revoker is the same as the revoked
     * certificate. After successful completion, the
     * certificate status will be 'SUSPENDED' and a new 'crr' db row will be
     * inserted with the specified reason and and 'APPROVED' crr status.
     * <p>
     * Note, there are no checks to ensure the certificate to be revoked is VALID and not-expired.
     *
     * @param cert_key_toRevoke PK of the certificate to revoke
     * @param reason
     * @return The ID/PK of the newly inserted db 'crr' row
     */
    @RolesAllowed({"ROLE_CERTOWNER"})
    @Transactional
    public long selfRevokeCertificate(long cert_key_toRevoke, String reason) {
        // throw early  
        if (cert_key_toRevoke <= 0) {
            throw new IllegalArgumentException("Invalid cert_key_toRevoke");
        }
        if (reason == null) {
            throw new IllegalArgumentException("Invalid reason");
        }

        // search for an existing CRR for this cert? 
        CertificateRow certRow = this.jdbcCertDao.findById(cert_key_toRevoke);
        if (certRow == null) {
            throw new RuntimeException("Invalid certificate row - no row found with id [" + cert_key_toRevoke + "]");
        }
        // update the certrow status 
        certRow.setStatus("SUSPENDED");
        // therefore also update data col last action date (note raop is -1 for self revoke)  
        certRow.setData(this.jdbcCertDao.updateDataCol_LastActionDateRaop(certRow.getData(), -1));
        if (this.jdbcCertDao.updateCertificateRow(certRow) != 1) {
            throw new RuntimeException("Multiple certificate rows attempted for udpate");
        }

        // build a new crr row (note raopId is null for self revoke) 
        CrrRow crrRow = this.buildCrrRow(certRow, reason, CRR_STATUS.APPROVED, null);
        if (this.jdbcCrrDao.insertCrrRow(crrRow) != 1) {
            throw new RuntimeException("Multiple crr rows attempted for udpate");
        }
        return crrRow.getCrr_key();
    }


    /**
     * Delete the revocation request that has the specified crr_key by the given raop.
     * After successful deletion, the crr status will be set to DELETED and the
     * corresponding certificate status will be set to VALID.
     * <p>
     * Note, existing crr status MUST be APPROVED or NEW.
     * Note, there are no checks to ensure the certificate status is SUSPENDED and not-expired.
     * There are no checks to determine if the raop is home ra (or not).
     *
     * @param crr_key_toDelete
     * @param raopId
     */
    @RolesAllowed({"ROLE_RAOP", "ROLE_CAOP"})
    @Transactional
    public void deleteRevocationRequest(long crr_key_toDelete, long raopId) {
        // throw early
        if (crr_key_toDelete <= 0) {
            throw new IllegalArgumentException("Invalid crr_key_toDelete");
        }
        if (raopId <= 0) {
            throw new IllegalArgumentException("Invalid raopId");
        }
        CrrRow crrRow = this.jdbcCrrDao.findById(crr_key_toDelete);
        if (crrRow == null) {
            throw new RuntimeException("Invalid crrRow - no row found with id " + crr_key_toDelete);
        }
        CertificateRow certRow = this.jdbcCertDao.findById(crrRow.getCert_key());
        if (certRow == null) {
            throw new RuntimeException("Invalid certRow - no row found with id " + crrRow.getCert_key());
        }
        // check existing certificate.status
        //if(!"SUSPENDED".equals(certRow.getStatus()) ){
        //    throw new RuntimeException("Invalid cerTrow - status should be SUSPENDED but was "+certRow.getStatus());
        //}
        // check existing crr.status
        if (!CRR_STATUS.APPROVED.toString().equals(crrRow.getStatus()) &&
                !CRR_STATUS.NEW.toString().equals(crrRow.getStatus())) {
            throw new RuntimeException("Invalid crrRow - status is not NEW or APPROVED: " + crr_key_toDelete);
        }

        // set the CRR status to deleted
        crrRow.setStatus(CRR_STATUS.DELETED.toString());
        // update the CRR data column
        crrRow.setData(
                this.jdbcCrrDao.updateDataCol_StatusRaop(crrRow.getData(), CRR_STATUS.DELETED.toString(), raopId)
                        .trim() + "\n");
        if (this.jdbcCrrDao.updateCrrRow(crrRow) != 1) {
            throw new RuntimeException("Multiple crr rows attempted for udpate");
        }
        // update the corresponding certrow status
        certRow.setStatus("VALID");
        // therefore also update the certrow data column last action date
        certRow.setData(this.jdbcCertDao.updateDataCol_LastActionDateRaop(certRow.getData(), raopId));
        if (this.jdbcCertDao.updateCertificateRow(certRow) != 1) {
            throw new RuntimeException("Multiple certificate rows attempted for udpate");
        }
    }

    /**
     * Approve the crr with the specified key by the given raop.
     * After successful approval, the crr status will be set to APPROVED.
     * <p>
     * Note, crr must have an existing status of NEW or APPROVED.
     * There are no checks to determine if the raop is home ra (or not).
     *
     * @param crr_key_toApprove
     * @param raopId
     */
    @RolesAllowed({"ROLE_RAOP", "ROLE_CAOP"})
    @Transactional
    public void approveRevocationRequest(long crr_key_toApprove, long raopId) {
        // throw early
        if (crr_key_toApprove <= 0) {
            throw new IllegalArgumentException("Invalid crr_key_toApprove");
        }
        if (raopId <= 0) {
            throw new IllegalArgumentException("Invalid raopId");
        }
        CrrRow crrRow = this.jdbcCrrDao.findById(crr_key_toApprove);
        if (crrRow == null) {
            throw new RuntimeException("Invalid crrRow - no row found with id " + crr_key_toApprove);
        }
        if (!CRR_STATUS.NEW.toString().equals(crrRow.getStatus()) && !CRR_STATUS.APPROVED.toString().equals(crrRow.getStatus())) {
            throw new RuntimeException("Invalid crrRow - expected crr status is NEW or APPROVED but was: " + crrRow.getStatus());
        }

        // set the CRR status to APPROVED
        crrRow.setStatus(CRR_STATUS.APPROVED.toString());
        // update the CRR data column TODO do we need to add a newline char
        crrRow.setData(
                this.jdbcCrrDao.updateDataCol_StatusRaop(crrRow.getData(), CRR_STATUS.APPROVED.toString(), raopId)
                        .trim() + "\n");
        if (this.jdbcCrrDao.updateCrrRow(crrRow) != 1) {
            throw new RuntimeException("Multiple crr rows attempted for udpate");
        }
        // no need to update the certrow
    }
    

   

    /* Sample CRR Data column
    -----BEGIN HEADER-----
    TYPE = CRR
    SERIAL = 209665
    RAOP = 3503
    DELETED_DATE = Wed Feb  6 17:08:30 2013 UTC
    -----END HEADER-----
    SUBMIT_DATE = Wed Feb  6 17:08:30 2013 UTC
    REVOKE_REASON = test
    REVOKE_CERTIFICATE_DN = CN=john dev kewley user,L=DL,OU=CLRC,O=eScienceDev,C=UK
    REVOKE_CERTIFICATE_NOTBEFORE = Feb  1 15:28:11 2013 GMT
    REVOKE_CERTIFICATE_NOTAFTER = Jul 31 15:28:11 2013 GMT
    REVOKE_CERTIFICATE_SERIAL = 3501
    REVOKE_CERTIFICATE_ISSUER_DN = CN=DevelopmentCA,OU=NGS,O=eScienceDev,C=UK
    REVOKE_CERTIFICATE_KEY_DIGEST = c4e5e6e1faffe38b37e96046d88aea4e
     */

    private CrrRow buildCrrRow(CertificateRow certRowToRevoke, String reason, CRR_STATUS newStatus, Long raopID) {
        CrrRow crrRow = new CrrRow();
        crrRow.setCrr_key(this.jdbcCrrDao.getNextCrr_key());
        crrRow.setCert_key(certRowToRevoke.getCert_key());
        crrRow.setCn(certRowToRevoke.getCn()); // check is cn of cert to be revoked
        crrRow.setDn(certRowToRevoke.getDn()); // check is rfc2253 dn of cert to be revoked
        crrRow.setFormat("CRR");
        crrRow.setStatus(newStatus.toString()); // depends on the requestor  
        String actionDate = crrDateFormat.format(new Date());
        crrRow.setSubmit_date(actionDate);
        crrRow.setReason(reason);

        Date notBefore, notAfter;
        String issuerDN;
        try {
            // get the expiry dates of the cert that is to be revoked 
            X509Certificate x509 = this.jdbcCertDao.getX509CertificateFromData(certRowToRevoke);
            notAfter = x509.getNotAfter();
            notBefore = x509.getNotBefore();
            issuerDN = x509.getIssuerX500Principal().toString();
        } catch (CertificateException | UnsupportedEncodingException ex) {
            throw new IllegalArgumentException(ex);
        }

        StringBuilder result = new StringBuilder();
        result.append("-----BEGIN HEADER-----").append("\n");
        result.append("TYPE = CRR").append("\n");
        //result.append("SERIAL = ").append(crrRow.getCert_key()).append("\n");
        // crr key not the certificate key 
        result.append("SERIAL = ").append(crrRow.getCrr_key()).append("\n");

        if (raopID != null) {
            result.append("RAOP = ").append(raopID).append("\n");
        }
        if (CRR_STATUS.DELETED.equals(newStatus)) {
            result.append("DELETED_DATE = ").append(actionDate).append("\n");
        }

        result.append("-----END HEADER-----").append("\n");
        result.append("SUBMIT_DATE = ").append(crrRow.getSubmit_date()).append("\n");
        result.append("REVOKE_REASON = ").append(crrRow.getReason()).append("\n");
        result.append("REVOKE_CERTIFICATE_DN = ").append(crrRow.getDn()).append("\n");
        result.append("REVOKE_CERTIFICATE_NOTBEFORE = ").append(notBefore).append("\n");
        result.append("REVOKE_CERTIFICATE_NOTAFTER = ").append(notAfter).append("\n");
        result.append("REVOKE_CERTIFICATE_SERIAL = ").append(crrRow.getCert_key()).append("\n");
        result.append("REVOKE_CERTIFICATE_ISSUER_DN = ").append(issuerDN).append("\n");
        result.append("REVOKE_CERTIFICATE_KEY_DIGEST = ??????????").append("\n");

        crrRow.setData(result.toString());
        return crrRow;
    }


    /**
     * @param jdbcCrrDao the jdbcCrrDao to set
     */
    @Inject
    public void setJdbcCrrDao(JdbcCrrDao jdbcCrrDao) {
        this.jdbcCrrDao = jdbcCrrDao;
    }

    @Inject
    public void setJdbcCertificateDao(JdbcCertificateDao jdbcCertDao) {
        this.jdbcCertDao = jdbcCertDao;
    }
}
