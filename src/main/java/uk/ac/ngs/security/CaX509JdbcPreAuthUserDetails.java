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
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import uk.ac.ngs.dao.JdbcCertificateDao;
import uk.ac.ngs.domain.CertificateRow;

import javax.inject.Inject;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * UserDetails service implementation that retrieves a UserDetails object based on a given
 * (pre)Authentication object which must be an X509Certificate.
 *
 * @author David Meredith
 */
public class CaX509JdbcPreAuthUserDetails implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

    private static final Log log = LogFactory.getLog(CaX509JdbcPreAuthUserDetails.class);
    private JdbcCertificateDao jdbcCertDao;
    private SecurityContextService securityContextService;
    private CaJdbcUserDetailsService caJdbcUserDetailsService;

    /**
     * Query CA database for user with given X509Certificate and load their roles.
     * If user is not found, throw a UsernameNotFoundException.
     *
     * @param token Must be castable to a {@link java.security.cert.X509Certificate} object
     * @return UserDetails (never null)
     */
    @Override
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) {
        // http://stackoverflow.com/questions/14563397/spring-security-x-509-authentication-without-user-service
        X509Certificate certificate = (X509Certificate) token.getCredentials();
        String dn = certificate.getSubjectDN().toString();
        // Convert provided DN to rfc2253 style (commas with no spaces) 
        // from: "CN=david meredith, L=DL, OU=CLRC, O=eScience, C=UK"
        // to: "CN=david meredith,L=DL,OU=CLRC,O=eScience,C=UK"
        dn = dn.replaceAll(", ", ",");
        BigInteger serial = certificate.getSerialNumber();
        log.info("Auth request for dn: [" + dn + "] serial: [" + serial.toString() + "]");
        CertificateRow cr = this.jdbcCertDao.findById(serial.longValue());

        if (cr != null) {
            List<GrantedAuthority> auths = this.securityContextService.getGrantedAuthorities(cr);
            return new CaUser(dn, true, true, true, true, auths, cr);
        }
        throw new UsernameNotFoundException("User Not found [" + dn + "] [" + serial.toString() + "]");
    }


    @Inject
    public void setJdbcCaUserAuthDao(JdbcCertificateDao jdbcCertDao) {
        this.jdbcCertDao = jdbcCertDao;
    }

    @Inject
    public void setSecurityContextService(SecurityContextService securityContextService) {
        this.securityContextService = securityContextService;
    }

    @Inject
    public void setCaJdbcUserDetailsService(CaJdbcUserDetailsService caJdbcUserDetailsService) {
        this.caJdbcUserDetailsService = caJdbcUserDetailsService;
    }

    /*@Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        return caJdbcUserDetailsService.loadUserByUsername(s);
    }*/
}
