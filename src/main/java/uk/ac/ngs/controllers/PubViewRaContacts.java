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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.HtmlUtils;
import uk.ac.ngs.common.CertUtil;
import uk.ac.ngs.dao.JdbcCertificateDao;
import uk.ac.ngs.dao.JdbcRaopListDao;
import uk.ac.ngs.domain.CertificateRow;
import uk.ac.ngs.domain.RaopListRow;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Controller for the <tt>/pub/viewRAs</tt> page.
 *
 * @author David Meredith
 */
@Controller
@RequestMapping("/pub/viewRAs")
public class PubViewRaContacts {
    private static final Log log = LogFactory.getLog(PubViewRaContacts.class);
    private JdbcRaopListDao jdbcRaopListDao;
    private JdbcCertificateDao certDao;
    private final Pattern negatedOuPattern = Pattern.compile("[^a-zA-Z0-9\\-]");


    public static class ViewRaContact {
        private final List<RaopListRow> raoplistRows;
        private final CertificateRow certRow;
        private final String loc;
        private final String ou;

        public ViewRaContact(CertificateRow certRow, String loc, String ou, List<RaopListRow> raoplistRows) {
            this.raoplistRows = raoplistRows;
            this.certRow = certRow;
            this.loc = loc;
            this.ou = ou;
        }

        /**
         * @return the certRow
         */
        public CertificateRow getCertRow() {
            return certRow;
        }

        /**
         * @return the raoplistRows
         */
        public List<RaopListRow> getRaoplistRows() {
            return raoplistRows;
        }

        /**
         * @return the loc
         */
        public String getLoc() {
            return loc;
        }

        /**
         * @return the ou
         */
        public String getOu() {
            return ou;
        }
    }

    /**
     * ModelAttribute annotations defined on a method in a controller are
     * invoked before RequestMapping methods, within the same controller.
     *
     * @param ou      OrgUnit value
     * @param model
     * @param session
     */
    @ModelAttribute
    public void populateModel(@RequestParam(required = false) String ou,
                              ModelMap model, HttpSession session) {
        log.debug("populateMessage");

        if (negatedOuPattern.matcher(ou).find()) {
            // provide an empty list instead
            model.put("ou", "Invalid ou param"); //["+HtmlUtils.htmlEscapeHex(ou)+"]");  
            //model.put("raRows", new ArrayList<RaopListRow>(0));
            model.put("contacts", new ArrayList<ViewRaContact>(0));
        } else {
            // escape reflected untrusted content   
            model.put("ou", HtmlUtils.htmlEscapeHex(ou));


            List<ViewRaContact> contacts = new ArrayList<>(0);
            List<CertificateRow> certRows = this.certDao.findActiveRAsBy(null, ou);
            for (CertificateRow certRow : certRows) {
                String loc = CertUtil.extractDnAttribute(certRow.getDn(), CertUtil.DNAttributeType.L);
                List<RaopListRow> raoplistRowsWithSameCnOuLoc = this.jdbcRaopListDao.findBy(ou, loc, certRow.getCn(), Boolean.TRUE);
                ViewRaContact contact = new ViewRaContact(certRow, loc, ou, raoplistRowsWithSameCnOuLoc);
                contacts.add(contact);
            }
            model.put("contacts", contacts);
        }
    }


    /**
     * Handle GETs to '/pub/viewralist' for Idempotent page refreshes.
     */
    @RequestMapping(method = RequestMethod.GET)
    public String handleGetRequest() {
        return "pub/viewRAs";
    }

    @Inject
    public void setJdbcRaopListDao(JdbcRaopListDao jdbcRaopListDao) {
        this.jdbcRaopListDao = jdbcRaopListDao;
    }


    @Inject
    public void setJdbcCertificateDao(JdbcCertificateDao dao) {
        this.certDao = dao;
    }
}
