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

import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import javax.servlet.http.HttpSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import uk.ac.ngs.common.PartialPagedListHolder;
import uk.ac.ngs.dao.JdbcRalistDao;
import uk.ac.ngs.domain.RalistRow;

/**
 * Controller for the
 * <code>/pub/viewralist</code> page
 *
 * @author David Meredith
 */
@Controller
@RequestMapping("/pub/viewralist")
public class PubViewRalist {

    private static final Log log = LogFactory.getLog(PubViewRalist.class);
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
        List<RalistRow> rows = this.ralistDao.findAllByActive(null, null, null); 
        //log.debug("ralist rows size: "+rows.size());
        PartialPagedListHolder<RalistRow> pagedListHolder = new PartialPagedListHolder<RalistRow>(rows); 
        model.addAttribute(RALIST_PAGE_LIST_HOLDER, pagedListHolder); 
        session.setAttribute(LAST_RALIST_SEARCH_DATE_SESSION, new Date());
    }


    /**
     * Handle GETs to '/pub/viewralist' for Idempotent page refreshes.
     */
    @RequestMapping(method = RequestMethod.GET)
    public String handleGetRequest() {
        return "pub/viewralist";
    }


    /**
     * @param ralistDao the ralistDao to set
     */
    @Inject
    public void setRalistDao(JdbcRalistDao ralistDao) {
        this.ralistDao = ralistDao;
    }
}
