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
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.ac.ngs.dao.JdbcRequestDao;
import uk.ac.ngs.domain.RequestRow;
import uk.ac.ngs.domain.RequestWrapper;
import uk.ac.ngs.forms.ViewBulkFormBean;
import uk.ac.ngs.security.CaUser;
import uk.ac.ngs.security.SecurityContextService;
import uk.ac.ngs.service.CsrManagerService;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing a bulk of Requests.
 *
 * @author Sam Worley
 * @author David Meredith
 */
@Controller
@RequestMapping("/raop/viewbulk")
@Secured("ROLE_RAOP")
public class ViewBulk {

    private JdbcRequestDao jdbcRequestDao;
    private CsrManagerService csrManagerService;
    private SecurityContextService securityContextService;
    private static final Log log = LogFactory.getLog(ViewBulk.class);

    /**
     * Populate the view model before each page is rendered.
     *
     * @param bulkId   Optional, can be null.
     * @param modelMap
     */
    @ModelAttribute
    public void populateModel(
            @RequestParam(value = "bulkId", required = false) Long bulkId, ModelMap modelMap) {
        log.debug("Populate Model");
        ViewBulkFormBean viewBulkFormBean = new ViewBulkFormBean();
        viewBulkFormBean.setRows(new ArrayList<RequestWrapper>(0));
        modelMap.put("bulkView", viewBulkFormBean);

        if (bulkId != null) {
            //find all certs in bulk
            Map<JdbcRequestDao.WHERE_PARAMS, String> whereParams = new EnumMap<JdbcRequestDao.WHERE_PARAMS, String>(JdbcRequestDao.WHERE_PARAMS.class);
            whereParams.put(JdbcRequestDao.WHERE_PARAMS.BULKID_EQ, "" + bulkId);
            List<RequestRow> otherBulks = this.jdbcRequestDao.findBy(whereParams, null, null);
            log.debug("Found [" + otherBulks.size() + "] request rows for bulkId [" + bulkId + "]");
            // Rather than pass a list of 'RequestRow' instances to view layer, we 
            // wrap each RequestRow within a wrapper 'transfer' 
            // object so we can add additional properties for the view layer 
            // e.g. 'checked'.  
            List<RequestWrapper> otherBulksWrapped = new ArrayList<RequestWrapper>(0);
            for (RequestRow otherBulk : otherBulks) {
                RequestWrapper rr = new RequestWrapper(false, otherBulk);
                otherBulksWrapped.add(rr);
            }

            viewBulkFormBean.setRows(otherBulksWrapped);
            viewBulkFormBean.setBulkId(bulkId);
            modelMap.put("bulkView", viewBulkFormBean);
        }
    }

    /**
     * Approve the selected Requests that belong to a bulk.
     *
     * @param bulkForm
     * @param result
     * @param redirectAttrs
     * @param model
     * @return
     */
    @RequestMapping(value = "/approveBulks", method = RequestMethod.POST)
    public String approveSelectedBulks(@ModelAttribute("bulkView") ViewBulkFormBean bulkForm, BindingResult result,
                                       RedirectAttributes redirectAttrs,
                                       Model model) {
        if (result.hasErrors()) {
            log.warn("binding and validation errors");
            return "raop/viewbulk";
        }
        long bulkId = bulkForm.getBulkId();
        List<RequestWrapper> bulkRows = bulkForm.getRows();
        List<Long> checkedIds = new ArrayList<Long>(0);
        log.debug("Bulks for APPROVE: [" + bulkRows.size() + "] for bulkId: [" + bulkId + "]");
        for (RequestWrapper rr : bulkRows) {
            log.debug("req_key: [" + rr.getRequestRow().getReq_key()
                    + "] cn: [" + rr.getRequestRow().getCn() + "] checked [" + rr.isChecked() + "]");
            if (rr.isChecked()) {
                checkedIds.add(rr.getRequestRow().getReq_key());
            }
        }

        if (checkedIds.isEmpty()) {
            redirectAttrs.addFlashAttribute("errorMessage",
                    "No bulks selected for approval");
        } else {
            // Approve the checked rows only
            CaUser caUser = securityContextService.getCaUserDetails();
            long raopId = caUser.getCertificateRow().getCert_key();
            List<Long> updatedCSRs = this.csrManagerService.
                    updateCsrStatusCollection(checkedIds, raopId, CsrManagerService.CSR_STATUS.APPROVED);

            // Add a message
            if (!updatedCSRs.isEmpty()) {
                redirectAttrs.addFlashAttribute("successMessage", "["
                        + updatedCSRs.size() + "] Approvals performed ok");
            } else {
                redirectAttrs.addFlashAttribute("errorMessage",
                        "No bulks were approved");
            }
        }

        // Add Re-direct attribute to the same page using the same bulkId  
        // note, after redirect, the populateModel function is called again which 
        // sets checked = false on all the rows. 
        if (bulkForm.getBulkId() != null) {
            redirectAttrs.addAttribute("bulkId", bulkForm.getBulkId());
        }
        return "redirect:/raop/viewbulk";
    }

    @RequestMapping(value = "/deleteBulks", method = RequestMethod.POST)
    public String deleteSelectedBulks(@ModelAttribute("bulkView") ViewBulkFormBean bulkForm, BindingResult result,
                                      RedirectAttributes redirectAttrs,
                                      Model model) {
        if (result.hasErrors()) {
            log.warn("binding and validation errors");
            return "raop/viewbulk";
        }
        long bulkId = bulkForm.getBulkId();
        List<RequestWrapper> bulkRows = bulkForm.getRows();
        List<Long> checkedIds = new ArrayList<Long>(0);
        log.debug("Bulks for DELETE: [" + bulkRows.size() + "] for bulkId: [" + bulkId + "]");
        for (RequestWrapper rr : bulkRows) {
            log.debug("req_key: [" + rr.getRequestRow().getReq_key()
                    + "] cn: [" + rr.getRequestRow().getCn() + "] checked [" + rr.isChecked() + "]");
            if (rr.isChecked()) {
                checkedIds.add(rr.getRequestRow().getReq_key());
            }
        }

        if (checkedIds.isEmpty()) {
            redirectAttrs.addFlashAttribute("errorMessage",
                    "No bulks selected for deletion");
        } else {
            // Delete the checked rows only
            CaUser caUser = securityContextService.getCaUserDetails();
            long raopId = caUser.getCertificateRow().getCert_key();
            List<Long> updatedCSRs = this.csrManagerService.
                    updateCsrStatusCollection(checkedIds, raopId, CsrManagerService.CSR_STATUS.DELETED);

            // Add a message
            if (!updatedCSRs.isEmpty()) {
                redirectAttrs.addFlashAttribute("successMessage", "["
                        + updatedCSRs.size() + "] Deletions performed ok");
            } else {
                redirectAttrs.addFlashAttribute("errorMessage",
                        "No bulks were deleted");
            }
        }

        // Add Re-direct attribute to the same page using the same bulkId  
        // note, after redirect, the populateModel function is called again which 
        // sets checked = false on all the rows. 
        if (bulkForm.getBulkId() != null) {
            redirectAttrs.addAttribute("bulkId", bulkForm.getBulkId());
        }
        return "redirect:/raop/viewbulk";
    }


    @RequestMapping(method = RequestMethod.GET)
    public void handleViewBulk(ModelMap modelMap) {
    }

    @Inject
    public void setJdbcRequestDao(JdbcRequestDao jdbcRequestDao) {
        this.jdbcRequestDao = jdbcRequestDao;
    }

    @Inject
    public void setSecurityContextService(SecurityContextService securityContextService) {
        this.securityContextService = securityContextService;
    }

    @Inject
    public void setCsrManagerService(CsrManagerService csrManagerService) {
        this.csrManagerService = csrManagerService;
    }
}
