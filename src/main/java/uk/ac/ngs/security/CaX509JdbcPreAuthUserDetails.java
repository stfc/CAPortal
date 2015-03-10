/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.ngs.security;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.inject.Inject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import uk.ac.ngs.dao.JdbcCertificateDao;
import uk.ac.ngs.domain.CertificateRow;

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
        throw new UsernameNotFoundException("User Not found [" + dn + "] ["+serial.toString()+"]");
    }

 
   
    @Inject
    public void setJdbcCaUserAuthDao(JdbcCertificateDao jdbcCertDao) {
        this.jdbcCertDao = jdbcCertDao;
    }
    
    @Inject
	public void setSecurityContextService(SecurityContextService securityContextService){
	    this.securityContextService = securityContextService; 
	}

}
