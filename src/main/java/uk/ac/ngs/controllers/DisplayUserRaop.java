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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import uk.ac.ngs.dao.JdbcCertificateDao;
import uk.ac.ngs.dao.JdbcCrrDao;
import uk.ac.ngs.dao.JdbcRaopListDao;
import uk.ac.ngs.dao.JdbcRequestDao;
import uk.ac.ngs.domain.CertificateRow;
import uk.ac.ngs.domain.CrrRow;
import uk.ac.ngs.domain.RaopListRow;
import uk.ac.ngs.domain.RequestRow;
import uk.ac.ngs.security.CaUser;
import uk.ac.ngs.security.SecurityContextService;
import uk.ac.ngs.service.CertUtil;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

//import org.springframework.security.core.GrantedAuthority;

/**
 * @author David Meredith
 */
@Controller
@RequestMapping("/raop/displayuserraop")
@Secured("ROLE_RAOP")
public class DisplayUserRaop {

    private static final Log log = LogFactory.getLog(DisplayUserRaop.class);
    private SecurityContextService securityContextService;
    private JdbcRaopListDao jdbcRaopListDao;
    private JdbcCertificateDao jdbcCertificateDao;

    public DisplayUserRaop(SecurityContextService securityContextService, JdbcRaopListDao jdbcRaopListDao,
            JdbcCertificateDao jdbcCertificateDao) {
        this.securityContextService = securityContextService;
        this.jdbcRaopListDao = jdbcRaopListDao;
        this.jdbcCertificateDao = jdbcCertificateDao;
    }


    @ModelAttribute
    public void populateModel(Model model) {
    log.debug("DisplayUserRaop populateModel");

    // Get current user and their DN
    CaUser caUser = securityContextService.getCaUserDetails();
    String currentUserDn = caUser.getCertificateRow().getDn();

    // Extract DN attributes
    String ou = CertUtil.extractDnAttribute(currentUserDn, CertUtil.DNAttributeType.OU);
    String o = CertUtil.extractDnAttribute(currentUserDn, CertUtil.DNAttributeType.O);
    String loc = CertUtil.extractDnAttribute(currentUserDn, CertUtil.DNAttributeType.L);

    // Build RA string and add to model
    String ra = String.format("%s %s", ou, loc);
    model.addAttribute("ra", ra);
    log.debug("ra is:[" + ra + "]");

    // Fetch active users and RA operators for the current RA
    List<CertificateRow> userRaopRows = jdbcCertificateDao.findActiveUserAndRAOperatorBy(ou, o, loc);

    // Exclude current user from the list
    List<CertificateRow> filteredRows = userRaopRows.stream()
        .filter(row -> !currentUserDn.equals(row.getDn()))
        .collect(Collectors.toList());

    log.debug("Filtered User and RA Operator rows size: " + filteredRows.size());
    model.addAttribute("userRaopRows", filteredRows);

    // Add timestamp
    model.addAttribute("lastPageRefreshDate", new Date());
}


    /**
     * Select the raop/displayuserraop view to render.
     *
     * @return raop/displayuserraop
     */
    @RequestMapping(method = RequestMethod.GET)
    public String displayUserRaop(Locale locale, Model model) {
        log.debug("Controller /raop/");
        return "raop/displayuserraop";
    }
}
