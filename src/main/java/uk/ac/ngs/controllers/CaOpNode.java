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
package uk.ac.ngs.controllers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import uk.ac.ngs.common.MutableConfigParams;
import uk.ac.ngs.dao.JdbcCrrDao;
import uk.ac.ngs.dao.JdbcRequestDao;
import uk.ac.ngs.domain.CrrRow;
import uk.ac.ngs.domain.RequestRow;
import uk.ac.ngs.security.SecurityContextService;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for importing and exporting CSRs/CRRs.
 *
 * @author David Meredith
 */
@Controller
@RequestMapping("/caop/node")
public class CaOpNode {

    private static final Log log = LogFactory.getLog(CaOpNode.class);
    private SecurityContextService securityContextService;
    private JdbcRequestDao jdbcRequestDao;
    private JdbcCrrDao jdbcCrrDao;
    private MutableConfigParams mutableConfigParams;

    @ModelAttribute
    public void populateModel(ModelMap model) throws IOException {
        log.debug("populateModel");
        model.addAttribute("lastViewRefreshDate", new Date());
        //CaUser caUser = securityContextService.getCaUserDetails();

        // Fetch approved CSRs/CRRs and populate model
        model.addAttribute("approvedCsrRows", this.getApprovedCsrRows());
        model.addAttribute("approvedCrrRows", this.getApprovedCrrRows());

        // Determine if import/export buttons should be disabled (or not) 
        // Disable Export button if a tarball is detected  
        model.put("disableExport", true); //disableExportButton()); 
        // Disable the Import button if a lockfile is detected 
        model.put("disableImport", true); //disableImportButton()); 
    }

    private List<RequestRow> getApprovedCsrRows() {
        Map<JdbcRequestDao.WHERE_PARAMS, String> csrWhereParams = new EnumMap<JdbcRequestDao.WHERE_PARAMS, String>(JdbcRequestDao.WHERE_PARAMS.class);
        csrWhereParams.put(JdbcRequestDao.WHERE_PARAMS.STATUS_EQ, "APPROVED");
        List<RequestRow> csrRows = this.jdbcRequestDao.findBy(csrWhereParams, null, null);
        csrRows = this.jdbcRequestDao.setDataNotBefore(csrRows);
        return csrRows;
    }

    private List<CrrRow> getApprovedCrrRows() {
        Map<JdbcCrrDao.WHERE_PARAMS, String> crrWhereParams = new EnumMap<JdbcCrrDao.WHERE_PARAMS, String>(JdbcCrrDao.WHERE_PARAMS.class);
        crrWhereParams.put(JdbcCrrDao.WHERE_PARAMS.STATUS_EQ, "APPROVED");
        List<CrrRow> crrRows = this.jdbcCrrDao.findBy(crrWhereParams, null, null);
        crrRows = this.jdbcCrrDao.setSubmitDateFromData(crrRows);
        //crrRows.get(0).getCert_key()
        return crrRows;
    }


    private boolean disableExportButton() throws IOException {
        // Disable if a tarball is detected (can't overwrite) 
        String exportFilePath = this.mutableConfigParams.getProperty("node.export.file");
        File exportFile = new File(exportFilePath);
        return exportFile.exists();
    }

    private boolean disableImportButton() {
        // does import.lock file exist? 
        // if exists return true to disable button
        // if not exists, return false to enable button
        return false;
    }

    @RequestMapping(method = RequestMethod.GET)
    public String handleGetRequest() {
        return "/caop/node";
    }

    @RequestMapping(value = "/export", method = RequestMethod.POST)
    public String nodeExport() {
        log.debug("export requested");
        return "redirect:/caop/node";
    }

    @RequestMapping(value = "/import", method = RequestMethod.POST)
    public String nodeImport() {
        log.debug("import requested");
        //Check for stuff to import
        //If nothing to import, delete caportal-import.lock to re-enable Import button
        //If true, start a global TX
        //On commit (success), delete caportal-import.lock to re-enable Import button 
        //On rollback (fail), leave lock file and show error to CAop in porta i/f (needs manual intervention) 
        return "redirect:/caop/node";
    }

    @Inject
    public void setSecurityContextService(SecurityContextService securityContextService) {
        this.securityContextService = securityContextService;
    }

    @Inject
    public void setJdbcRequestDao(JdbcRequestDao jdbcRequestDao) {
        this.jdbcRequestDao = jdbcRequestDao;
    }

    @Inject
    public void setJdbcRequestDao(JdbcCrrDao jdbcCrrDao) {
        this.jdbcCrrDao = jdbcCrrDao;
    }

    @Inject
    public void setMutableConfigParams(MutableConfigParams mutableConfigParams) {
        this.mutableConfigParams = mutableConfigParams;
    }
}
