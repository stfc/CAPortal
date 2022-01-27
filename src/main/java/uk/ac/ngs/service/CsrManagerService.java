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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ngs.dao.JdbcRequestDao;
import uk.ac.ngs.domain.RequestRow;
import uk.ac.ngs.exceptions.IllegalCsrStateTransition;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Service class for managing CSRs including deletion and approval.
 *
 * @author David Meredith
 */
@Service
public class CsrManagerService {
    private static final Log log = LogFactory.getLog(CsrManagerService.class);

    public enum CSR_STATUS {

        APPROVED, NEW, RENEW, DELETED
    }

    private JdbcRequestDao jdbcRequestDao;
    //private final static Pattern DATA_LAD_PATTERN = Pattern.compile("LAST_ACTION_DATE\\s?=\\s?(.+)$", Pattern.MULTILINE);
    // Date will be of the form:  Tue Apr 23 13:47:13 2013 UTC 

    public CsrManagerService() {
    }

    /**
     * Update the CSR status with the given csrSerialId to have the given CSR_STATUS value.
     * Only the following status transitions are supported:
     * <ul>
     *   <li>If the requested newStatus is 'APPROVED' the existing csr status must
     *   be 'NEW' or 'RENEW'</li>
     *   <li>If the requested newStatus is 'DELETED' the existing csr status can be any value.</li>
     * </ul>
     *
     * @param csrSerialId The PK of the {@link RequestRow} to update
     * @param raopId      The RA operator PK who is making the update
     * @param newStatus   The newly requested CSR status
     * @throws IllegalCsrStateTransition On an unknown/invalid status transition
     */
    @RolesAllowed({"ROLE_RAOP", "ROLE_CAOP"})
    @Transactional(rollbackFor = {IllegalCsrStateTransition.class})
    public void updateCsrStatus(long csrSerialId, long raopId, CSR_STATUS newStatus)
            throws IllegalCsrStateTransition {
        // Note, you don't need to include RuntimeException.class in the rollbackFor 
        // attribute in @Transactional, as per:
        // http://stackoverflow.com/questions/12830364/does-specifying-transactional-rollbackfor-also-include-runtimeexception
        RequestRow csr = this.jdbcRequestDao.findById(csrSerialId);
        this.updateCsrStatusHelper(csr, raopId, newStatus);
    }


    /**
     * For each CSR that has an ID/serial PK in the list, update the CSR status
     * to the given CSR_STATUS value.
     * For the allowed CSR status transitions see:
     * {@link #updateCsrStatus(long, long, uk.ac.ngs.service.CsrManagerService.CSR_STATUS) }
     * If any of the requested CSR status transitions is illegal, then it is
     * ignored and the next CSR is processed.
     *
     * @param csrSerialIds The PKs of the {@link RequestRow} instances to update.
     * @param raopId       The RA operator PK who is making the update
     * @param newStatus    The newly requested CSR status
     * @return A list of the CSR IDs that were updated
     */
    @RolesAllowed({"ROLE_RAOP", "ROLE_CAOP"})
    @Transactional
    public List<Long> updateCsrStatusCollection(List<Long> csrSerialIds, long raopId, CSR_STATUS newStatus) {
        log.info("Bulk updateCsrStatusCollection by RAOP: [" + raopId + "]");
        List<Long> updatedSerials = new ArrayList<>(0);
        for (Long csrSerialId : csrSerialIds) {
            RequestRow csr = this.jdbcRequestDao.findById(csrSerialId);
            try {
                int updated = this.updateCsrStatusHelper(csr, raopId, newStatus);
                if (updated == 1) {
                    updatedSerials.add(csr.getReq_key());
                }
            } catch (IllegalCsrStateTransition ex) {
                log.warn("Catching IllegalStateTransition: " + ex.getMessage());
                // do nothing, its likley that this particular csr 
                // has already been updated/approved by another RA 
            }
        }
        return updatedSerials;
    }

    /**
     * For all the CSRs that have the specified bulkId, update each CSR status
     * to the given CSR_STATUS value.
     * For the allowed CSR status transitions see:
     * {@link #updateCsrStatus(long, long, uk.ac.ngs.service.CsrManagerService.CSR_STATUS) }
     * If any of the requested CSR status transitions is illegal, then it is
     * ignored and the next CSR is processed.
     *
     * @param bulkId
     * @param raopId    The RA operator PK who is making the update
     * @param newStatus The newly requested CSR status
     * @return A list of the CSR IDs that were updated
     */
    @RolesAllowed({"ROLE_RAOP", "ROLE_CAOP"})
    @Transactional
    public List<Long> updateCsrStatusAllInBulk(long bulkId, long raopId, CSR_STATUS newStatus) {
        log.info("Bulk updateCsrStatusAllInBulk by RAOP: [" + raopId + "] on bulk: [" + bulkId + "]");
        List<Long> updatedSerials = new ArrayList<>(0);
        // Create a map used to define our where clauses (where bulkid = '5' and status = 'valid') 
        Map<JdbcRequestDao.WHERE_PARAMS, String> whereParams =
                new EnumMap<>(JdbcRequestDao.WHERE_PARAMS.class);
        whereParams.put(JdbcRequestDao.WHERE_PARAMS.BULKID_EQ, "" + bulkId);
        // Query the DB for related bulks
        List<RequestRow> otherBulks = this.jdbcRequestDao.findBy(whereParams, null, null);
        for (RequestRow csr : otherBulks) {
            try {
                int updated = this.updateCsrStatusHelper(csr, raopId, newStatus);
                if (updated == 1) {
                    updatedSerials.add(csr.getReq_key());
                }
            } catch (IllegalCsrStateTransition ex) {
                log.warn("Catching IllegalStateTransition: " + ex.getMessage());
                // do nothing, its likley that this particular csr 
                // has already been updated/approved by another RA 
            }
        }
        return updatedSerials;
    }

    /**
     * Update the given RequestRow status. For the allowed CSR status transitions see:
     * {@link #updateCsrStatus(long, long, uk.ac.ngs.service.CsrManagerService.CSR_STATUS) }
     * <p>
     * Important; the IllegalCsrStateTransition is thrown early <b>before</b> the csr row is
     * updated. You may/may-not need to trap this and re-throw as runtime
     * exception if running in a parent transaction.
     * <p/>
     *
     * @param csr
     * @param raopId
     * @param newStatus
     * @return Number of rows updated (should always be 1)
     * @throws IllegalCsrStateTransition on an illegal status transition.
     */
    private int updateCsrStatusHelper(RequestRow csr, long raopId, CSR_STATUS newStatus)
            throws IllegalCsrStateTransition {
        //RequestRow csr = this.jdbcRequestDao.findById(csrSerialId);
        String currentCsrStatus = csr.getStatus();
        if (newStatus.equals(CSR_STATUS.APPROVED)
                && ("NEW".equalsIgnoreCase(currentCsrStatus) || "RENEW".equalsIgnoreCase(currentCsrStatus))) {
            csr.setStatus(newStatus.toString());
            // Ensure that the data column ends with a newline; signing requires 
            // that data column is terminated by a newline otherwise signing 
            // can fail when exporting/concating records into single file. 
            csr.setData(this.jdbcRequestDao.updateDataCol_LastActionDateRaop(csr.getData(), raopId)
                    .trim() + "\n");
            return this.jdbcRequestDao.updateRequestRow(csr);

        } else if (newStatus.equals(CSR_STATUS.DELETED)) {
            //   if("NEW".equals(csr.getStatus()) || "RENEW".equals(csr.getStatus()) || "APPROVED".equals(csr.getStatus())){

            csr.setStatus(newStatus.toString());
            // Note, not sure we need a newline terminator but will add one 
            // to be consistent with above. 
            csr.setData(this.jdbcRequestDao.updateDataCol_LastActionDateRaop(csr.getData(), raopId)
                    .trim() + "\n");
            return this.jdbcRequestDao.updateRequestRow(csr);

        } else {
            String detail = "serial: [" + csr.getReq_key() + "] newStatus: [" + newStatus + "] current csrStatus: [" + currentCsrStatus + "]";
            throw new IllegalCsrStateTransition("Invalid status transition for CSR: " + detail);
        }
    }

    /**
     * Insert a new row into the 'request' table with the values from the given RequetRow (csr).
     * The given RequestRow must have a populated <code>req_key</code> value as
     * this is the PK which has to be provided, use: {@link #getNextPrimaryKey()}
     * to get the next value.
     *
     * @param csr
     */
    @Transactional
    public void insertCsr(RequestRow csr) {
        this.jdbcRequestDao.insertRequestRow(csr);
    }

    @Inject
    public void setJdbcRequestDao(JdbcRequestDao jdbcRequestDao) {
        this.jdbcRequestDao = jdbcRequestDao;
    }
}
