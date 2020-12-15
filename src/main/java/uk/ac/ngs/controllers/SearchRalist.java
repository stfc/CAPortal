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
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.ac.ngs.common.PartialPagedListHolder;
import uk.ac.ngs.dao.JdbcRalistDao;
import uk.ac.ngs.domain.RalistRow;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.List;

/**
 * Controller for the
 * <code>/raop/searchralist</code> page
 *
 * @author David Meredith
 */
@Controller
@RequestMapping("/raop/searchralist")
@Secured("ROLE_RAOP")
public class SearchRalist {

    private static final Log log = LogFactory.getLog(SearchRalist.class);
    private JdbcRalistDao ralistDao;
    public static final String RALIST_PAGE_LIST_HOLDER = "editRalistFormBean";
    public static final String LAST_RALIST_SEARCH_DATE_SESSION = "lastRalistSearchDate_session";

    /**
     * ModelAttribute annotations defined on a method in a controller are
     * invoked before RequestMapping methods, within the same controller.
     *
     * @param model
     * @param session
     */
    @ModelAttribute
    public void populateModel(Model model, HttpSession session) {
        // Populate model with an empty list if session var is not present (e.g. session expired) 
        //PartialPagedListHolder<RalistRow> pagedListHolder = (PartialPagedListHolder<RalistRow>) session.getAttribute(RALIST_PAGE_LIST_HOLDER);
        //if (pagedListHolder == null) {
        //    pagedListHolder = new PartialPagedListHolder<RalistRow>(new ArrayList<RalistRow>(0));
        //    session.setAttribute(RALIST_PAGE_LIST_HOLDER, pagedListHolder);
        //}
        List<RalistRow> rows = this.ralistDao.findAllByActive(null, null, null);
        //log.debug("ralist rows size: "+rows.size());
        PartialPagedListHolder<RalistRow> pagedListHolder = new PartialPagedListHolder<RalistRow>(rows);
        model.addAttribute(RALIST_PAGE_LIST_HOLDER, pagedListHolder);
        session.setAttribute(LAST_RALIST_SEARCH_DATE_SESSION, new Date());
    }


    /**
     * Handle GETs to <pre>/raop/searchralist</pre> for Idempotent page refreshes.
     */
    @RequestMapping(method = RequestMethod.GET)
    public String handleGetRequest() {
        return "raop/searchralist";
    }


    /**
     * Handle POSTs to <pre>/raop/searchralist/save</pre>
     */
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public String submitEditRalist(
            @ModelAttribute(RALIST_PAGE_LIST_HOLDER) PartialPagedListHolder<RalistRow> editRalistForm,
            BindingResult result,
            RedirectAttributes redirectAttrs, SessionStatus sessionStatus,
            Model model, HttpSession session) {

        log.debug("save");
        List<RalistRow> updatedRows = editRalistForm.getSource();
        //log.debug("posted rows size: "+updatedRows.size()); 
        if (updatedRows != null && updatedRows.size() > 0) {
            // here iterate all the rows and issue update statements in a single tx  
//            for(RalistRow row : updatedRows){
//                //if(!row.getActive()){
//                //    log.debug(row.getRa_id()); 
//                //}
//            }
        }

        return "redirect:/raop/searchralist";
    }


    /**
     * @param ralistDao the ralistDao to set
     */
    @Inject
    public void setRalistDao(JdbcRalistDao ralistDao) {
        this.ralistDao = ralistDao;
    }
}
