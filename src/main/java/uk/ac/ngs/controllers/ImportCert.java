/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.ngs.controllers;

import java.util.Locale;
import javax.inject.Inject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import uk.ac.ngs.dao.JdbcRequestDao;
import uk.ac.ngs.security.SecurityContextService;

/**
 *
 * @author jza23618
 */
@Controller
@RequestMapping("/caop/importcert")
public class ImportCert {
    private static final Log log = LogFactory.getLog(ImportCert.class);
    private JdbcRequestDao jdbcRequestDao;
    private SecurityContextService securityContextService;
    
    public ImportCert() {
    }
    
    @ModelAttribute
    public void populateModel(Model model) {
        log.debug("caop import populate model"); 
    }
    
    @RequestMapping(method = RequestMethod.GET)
    public String raAdminHome(Locale locale, Model model) {
        log.debug("Controller /caop/exportcert");
        return "caop/importcert";
    }
    
    @Inject
    public void setSecurityContextService(SecurityContextService securityContextService) {
        this.securityContextService = securityContextService;
    }
    
    @Inject
    public void setJdbcRequestDao(JdbcRequestDao jdbcRequestDao) {
        this.jdbcRequestDao = jdbcRequestDao;
    }
}
