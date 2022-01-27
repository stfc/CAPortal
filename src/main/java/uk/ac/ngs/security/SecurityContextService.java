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
package uk.ac.ngs.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import uk.ac.ngs.dao.JdbcCertificateDao;
import uk.ac.ngs.domain.CertificateRow;

import javax.inject.Inject;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stateless service to return the user's current authentication information.
 *
 * @author David Meredith
 */
public class SecurityContextService {
    private static final Log log = LogFactory.getLog(SecurityContextService.class);

    private final static Pattern DATA_ROLE_PATTERN = Pattern.compile("^\\s*ROLE\\s*=\\s*(.+)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private JdbcCertificateDao jdbcCertDao;

    /**
     * Get the current user's CaUser object or null if the user has not been authenticated.
     *
     * @return
     */
    public CaUser getCaUserDetails() {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof CaUser) {
                return (CaUser) principal;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Get the current users's user name or null if the user has not been authenticated.
     *
     * @return user name (DN)
     */
    public String getUserName() {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String username;
            if (principal instanceof UserDetails) {
                username = ((UserDetails) principal).getUsername();
            } else {
                username = principal.toString();
            }
            return username;
        } else {
            return null;
        }
    }


    public X509Certificate getCredentials() {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return (X509Certificate) SecurityContextHolder.getContext().getAuthentication().getCredentials();
        } else {
            return null;
        }
    }

    // org.springframework.security.web.authentication.WebAuthenticationDetails
    //Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();


    /**
     * If there is a currently authenticated principal, update the granted
     * authorities and the current Authentication token in the Spring security context.
     * If there is no currently authenticated principal, do nothing.
     * A RuntimeException is thrown and the current authentication context
     * is removed if:
     * <ul>
     *   <li>The Authentication token Principle is not of the expected type (e.g. CaUser instance)</li>
     *   <li>The Credentials are not of the expected type (e.g. X509Certificate).</li>
     * </ul>
     */
    public void refreshCurrentAuthenticationToken() {
        CaUser caUser = this.getCaUserDetails();
        if (caUser == null) {
            return;
        }

        X509Certificate cred = this.getCredentials();
        if (cred == null) {
            SecurityContextHolder.getContext().setAuthentication(null);
            throw new RuntimeException("Credentials are null");
        }
        long cert_key = this.getCaUserDetails().getCertificateRow().getCert_key();
        if (cert_key <= 0) {
            SecurityContextHolder.getContext().setAuthentication(null);
            throw new RuntimeException("cert_key has unexpected value [" + cert_key + "]");
        }

        // User may have been revoked in the DB so update the Spring  
        // authentication context using the newly revoked cert status changes. 
        //long cert_key = caUser.getCertificateRow().getCert_key();
        // re-fetch the cert row as certificate.status has been updated by revocation 
        CertificateRow cr = jdbcCertDao.findById(cert_key);
        // rerefresh the granted authorities based on the newly updated cert row
        List<GrantedAuthority> newAuths = this.getGrantedAuthorities(cr);

        // Create a new CaUser using new granted authorities and certrow 
        //Object principle = SecurityContextHolder.getContext().getAuthentication().getPrincipal(); //CaUser
        CaUser principle = new CaUser(cr.getDn(), true, true, true, true, newAuths, cr);

        // Update the currently authenticated principal
        Authentication token = new PreAuthenticatedAuthenticationToken(principle, cred, newAuths);
        SecurityContextHolder.getContext().setAuthentication(token);
    }


    public List<GrantedAuthority> getGrantedAuthorities(CertificateRow cr) {
        // Get the status and determine the relevant roles cr.getStatus(); 
        List<GrantedAuthority> auths = new ArrayList<>();
        String role = getRoleFromDataColumn(cr);
        if ("VALID".equals(cr.getStatus())) {
            if ("User".equalsIgnoreCase(role)) {
                SimpleGrantedAuthority co = new SimpleGrantedAuthority("ROLE_CERTOWNER");
                auths.add(co);

            } else if ("RA Operator".equalsIgnoreCase(role)) {
                SimpleGrantedAuthority ra = new SimpleGrantedAuthority("ROLE_RAOP");
                SimpleGrantedAuthority co = new SimpleGrantedAuthority("ROLE_CERTOWNER");
                auths.add(co);
                auths.add(ra);

            } else if ("CA Operator".equalsIgnoreCase(role)) {
                SimpleGrantedAuthority ca = new SimpleGrantedAuthority("ROLE_CAOP");
                SimpleGrantedAuthority ra = new SimpleGrantedAuthority("ROLE_RAOP");
                SimpleGrantedAuthority co = new SimpleGrantedAuthority("ROLE_CERTOWNER");
                auths.add(co);
                auths.add(ra);
                auths.add(ca);
            }
        } else {
            // certs with status != VALID can only have ROLE_CERTOWNER
            if ("User".equalsIgnoreCase(role)) {
                SimpleGrantedAuthority co = new SimpleGrantedAuthority("ROLE_CERTOWNER");
                auths.add(co);

            } else if ("RA Operator".equalsIgnoreCase(role)) {
                SimpleGrantedAuthority co = new SimpleGrantedAuthority("ROLE_CERTOWNER");
                auths.add(co);

            } else if ("CA Operator".equalsIgnoreCase(role)) {
                SimpleGrantedAuthority co = new SimpleGrantedAuthority("ROLE_CERTOWNER");
                auths.add(co);
            }
        }
        return auths;
    }


    /**
     * Extract the value from the 'ROLE = role_value' serialized in data field
     * of the given row.
     *
     * @param row
     * @return the ROLE value (if any) otherwise null.
     */
    private String getRoleFromDataColumn(CertificateRow row) {
        if (row.getData() != null) {
            Matcher m = DATA_ROLE_PATTERN.matcher(row.getData());
            if (m.find()) {
                return m.group(1).trim();
            }
        }
        return null;
    }


    @Inject
    public void setJdbcCaUserAuthDao(JdbcCertificateDao jdbcCertDao) {
        this.jdbcCertDao = jdbcCertDao;
    }


}
