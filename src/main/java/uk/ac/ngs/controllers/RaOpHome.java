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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import uk.ac.ngs.dao.JdbcCrrDao;
import uk.ac.ngs.dao.JdbcRaopListDao;
import uk.ac.ngs.dao.JdbcRequestDao;
import uk.ac.ngs.domain.CrrRow;
import uk.ac.ngs.domain.RaopListRow;
import uk.ac.ngs.domain.RequestRow;
import uk.ac.ngs.security.CaUser;
import uk.ac.ngs.security.SecurityContextService;
import uk.ac.ngs.service.CertUtil;

/**
 * @author David Meredith
 */
@Controller
@RequestMapping("/raop")
public class RaOpHome {

    private static final Log log = LogFactory.getLog(RaOpHome.class);
    private SecurityContextService securityContextService;
    private JdbcRaopListDao jdbcRaopListDao;
    private JdbcRequestDao jdbcRequestDao;
    private JdbcCrrDao jdbcCrrDao;

    public RaOpHome() {
        log.debug(RaOpHome.class.getName()+" ***created dave***"); 
    }


    
    
    @ModelAttribute
    public void populateModel(Model model) {
        log.debug("raop populateModel");

        // Get the current CaUser (so we can extract their DN)  
        CaUser caUser = securityContextService.getCaUserDetails();
        //Collection<GrantedAuthority> auths = caUser.getAuthorities();

        //model.addAttribute("caUser", caUser);

        // Extract the RA value from the user's certificate DN
        String dn = caUser.getCertificateRow().getDn();
        String OU = CertUtil.extractDnAttribute(dn, CertUtil.DNAttributeType.OU); //CLRC
        String L = CertUtil.extractDnAttribute(dn, CertUtil.DNAttributeType.L); //RAL
        String CN = CertUtil.extractDnAttribute(dn, CertUtil.DNAttributeType.CN); //meryem tahar
        String ra = OU + " " + L;
        model.addAttribute("ra", ra);
        log.debug("ra is:[" + ra + "]");

        // Get the current user's RAOP details for display (i.e. when they last 
        // did the training etc). Note, this does not affect their ability to 
        // do raop stuff in the portal. 
        List<RaopListRow> raoprows = this.jdbcRaopListDao.findBy(OU, L, CN, true);
        log.debug("raoprows size: " + raoprows.size());
        model.addAttribute("raoprows", raoprows);

        // Fetch a list of pending CSRs for the RA (NEW and RENEW) 
        // NEW
        Map<JdbcRequestDao.WHERE_PARAMS, String> whereParams = new HashMap<JdbcRequestDao.WHERE_PARAMS, String>();
        whereParams.put(JdbcRequestDao.WHERE_PARAMS.RA_EQ, ra);
        whereParams.put(JdbcRequestDao.WHERE_PARAMS.STATUS_EQ, "NEW");
        List<RequestRow> newRequestRows = jdbcRequestDao.findBy(whereParams, null, null);
        newRequestRows = jdbcRequestDao.setDataNotBefore(newRequestRows);
        model.addAttribute("new_reqrows", newRequestRows);
        // RENEW
        whereParams.put(JdbcRequestDao.WHERE_PARAMS.STATUS_EQ, "RENEW");
        List<RequestRow> renewRequestRows = jdbcRequestDao.findBy(whereParams, null, null);
        renewRequestRows = jdbcRequestDao.setDataNotBefore(renewRequestRows);
        model.addAttribute("renew_reqrows", renewRequestRows);

        // Fetch a list of pending CRRs for the RA 
        Map<JdbcCrrDao.WHERE_PARAMS, String> crrWhereParams = new HashMap<JdbcCrrDao.WHERE_PARAMS, String>();
        crrWhereParams.put(JdbcCrrDao.WHERE_PARAMS.STATUS_EQ, "NEW"); //NEW,APPROVED,ARCHIVED,DELETED 
        crrWhereParams.put(JdbcCrrDao.WHERE_PARAMS.DN_LIKE, "%L="+L+",OU="+OU+"%"); 
        List<CrrRow> crrRows = jdbcCrrDao.findBy(crrWhereParams, null, null);
        log.debug("crrRows size: ["+crrRows.size()+"]"); 
        crrRows = jdbcCrrDao.setSubmitDateFromData(crrRows); 
        model.addAttribute("crr_reqrows", crrRows);

        model.addAttribute("lastPageRefreshDate", new Date()); 
    }

    /**
     * Select the raop/home view to render.
     *
     * @return raop/home
     */
    @RequestMapping(method = RequestMethod.GET)
    public String raAdminHome(Locale locale, Model model) {
        log.debug("Controller /raop/");
        return "raop/raophome";
    }

    @Inject
    public void setSecurityContextService(SecurityContextService securityContextService) {
        this.securityContextService = securityContextService;
    }

    @Inject
    public void setJdbcRaopListDao(JdbcRaopListDao jdbcRaopListDao) {
        this.jdbcRaopListDao = jdbcRaopListDao;
    }

    @Inject
    public void setJdbcRequestDao(JdbcRequestDao jdbcRequestDao) {
        this.jdbcRequestDao = jdbcRequestDao;
    }

    @Inject
    public void setJdbcCrrDao(JdbcCrrDao jdbcCrrDao) {
        this.jdbcCrrDao = jdbcCrrDao;
    }
}
