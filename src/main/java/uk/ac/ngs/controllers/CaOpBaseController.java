package uk.ac.ngs.controllers;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

//import uk.ac.ngs.security.CaUser;
import uk.ac.ngs.security.SecurityContextService;
import uk.ac.ngs.service.CertUtil;

/**
 * 
 * @author Josh Hadley
 * @author David Meredith
 * 
 */
@Controller
@RequestMapping("/caop")
public class CaOpBaseController {
    private static final Log log = LogFactory.getLog(CaOpBaseController.class);
    private SecurityContextService securityContextService;
    private JdbcRaopListDao jdbcRaopListDao;
    private JdbcRequestDao jdbcRequestDao;
    private JdbcCrrDao jdbcCrrDao;
    
    @ModelAttribute
    public void populateModel(Model model) {
        log.debug("caop populateModel");
        // Fetch the certificateRow entry for display in the model
        CaUser caUser = securityContextService.getCaUserDetails();
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
        // APPROVED
        Map<JdbcRequestDao.WHERE_PARAMS, String> whereParams = new HashMap<JdbcRequestDao.WHERE_PARAMS, String>();
        whereParams.put(JdbcRequestDao.WHERE_PARAMS.RA_EQ, ra);
        whereParams.put(JdbcRequestDao.WHERE_PARAMS.STATUS_EQ, "APPROVED");
        List<RequestRow> newRequestRows = jdbcRequestDao.findBy(whereParams, null, null);
        newRequestRows = jdbcRequestDao.setDataNotBefore(newRequestRows);
        model.addAttribute("approved_reqrows", newRequestRows);

        // Fetch a list of pending CRRs for the RA 
        Map<JdbcCrrDao.WHERE_PARAMS, String> crrWhereParams = new HashMap<JdbcCrrDao.WHERE_PARAMS, String>();
        crrWhereParams.put(JdbcCrrDao.WHERE_PARAMS.STATUS_EQ, "APPROVED"); //NEW,APPROVED,ARCHIVED,DELETED 
        crrWhereParams.put(JdbcCrrDao.WHERE_PARAMS.DN_LIKE, "%L="+L+",OU="+OU+"%"); 
        List<CrrRow> crrRows = jdbcCrrDao.findBy(crrWhereParams, null, null);
        log.debug("crrRows size: ["+crrRows.size()+"]"); 
        crrRows = jdbcCrrDao.setSubmitDateFromData(crrRows); 
        model.addAttribute("crr_reqrows", crrRows);

        model.addAttribute("lastPageRefreshDate", new Date()); 
    }  

    /**
     * Respond to /caop render by returning its name.
     * 
     * @return caop/home
     */
    @RequestMapping(method = RequestMethod.GET)
    public String caAdminHome(Locale locale, Model model) {
        log.debug("Controller /caop/");
        return "caop/caophome";
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
