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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import uk.ac.ngs.dao.JdbcCaUserAuthDao;
import uk.ac.ngs.domain.CertificateRow;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

//import org.springframework.security.core.userdetails.memory.InMemoryDaoImpl;

/**
 * Custom UserDetailsService to authenticate user DN strings against users
 * in the CA database.
 *
 * @author David Meredith
 */
public class CaJdbcUserDetailsService implements UserDetailsService {
    private static final Log log = LogFactory.getLog(CaJdbcUserDetailsService.class);
    private final static Pattern DATA_ROLE_PATTERN = Pattern.compile("^\\s*ROLE\\s*=\\s*(.+)$", Pattern.MULTILINE);


    private JdbcCaUserAuthDao jdbcCaUserAuthDao;
    private SecurityContextService securityContextService;

    /**
     * Query CA database for user with given username (DN) and load their roles.
     *
     * @param dn The certificate DN of the form 'CN=david meredith, L=DL, OU=CLRC, O=eScience, C=UK'
     * @return The user details implementation.
     * @see org.springframework.security.core.userdetails.UserDetailsService#loadUserByUsername(java.lang.String)
     */
    @Override
    public UserDetails loadUserByUsername(String dn) throws UsernameNotFoundException {
        log.debug("Auth request for: [" + dn + "]");
        if (dn != null && !dn.trim().equals("")) {
            // Convert provided DN to rfc2253 style (commas with no spaces) 
            // from: "CN=david meredith, L=DL, OU=CLRC, O=eScience, C=UK"
            // to: "CN=david meredith,L=DL,OU=CLRC,O=eScience,C=UK"
            dn = dn.replaceAll(", ", ",");
            List<CertificateRow> rows = this.jdbcCaUserAuthDao.getCaUserEntry(dn);
            if (rows.size() > 0) {
                if (rows.size() > 1) {
                    log.warn("User has more than a single CertificateRow entry rows: [" + rows.size() + "] dn: [" + dn + "]");
                }
                // if multiple cert rows were returned, then we should always 
                // use the row that has the latest expiry date ! 
                CertificateRow cr = rows.get(0);
                Date max = rows.get(0).getNotAfter();
                for (CertificateRow testCr : rows) {
                    if (testCr.getNotAfter().after(max)) {
                        cr = testCr;
                    }
                }

                List<GrantedAuthority> auths = this.securityContextService.getGrantedAuthorities(cr);
                return new CaUser(dn, true, true, true, true, auths, cr);
            }
        }
        throw new UsernameNotFoundException("User Not found [" + dn + "]");
    }

    @Inject
    public void setJdbcCaUserAuthDao(JdbcCaUserAuthDao jdbcCaUserAuthDao) {
        this.jdbcCaUserAuthDao = jdbcCaUserAuthDao;
    }

    @Inject
    public void setSecurityContextService(SecurityContextService securityContextService) {
        this.securityContextService = securityContextService;
    }
}
