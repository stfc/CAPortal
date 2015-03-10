package uk.ac.ngs.security;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import uk.ac.ngs.dao.JdbcCaUserAuthDao;
import uk.ac.ngs.domain.CertificateRow;

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

    /**
     * Query CA database for user with given username (DN) and load their roles. 
     * 
     * @see org.springframework.security.core.userdetails.UserDetailsService#loadUserByUsername(java.lang.String)
     * @param dn The certificate DN of the form 'CN=david meredith, L=DL, OU=CLRC, O=eScience, C=UK'
     * @return The user details implementation. 
     */
    @Override
    public UserDetails loadUserByUsername(String dn) throws UsernameNotFoundException {
        log.debug("Auth request for: [" + dn + "]");
        if (dn != null && !dn.trim().equals("")){
            // Convert provided DN to rfc2253 style (commas with no spaces) 
            // from: "CN=david meredith, L=DL, OU=CLRC, O=eScience, C=UK"
            // to: "CN=david meredith,L=DL,OU=CLRC,O=eScience,C=UK"
            dn = dn.replaceAll(", ", ","); 
            List<CertificateRow> rows = this.jdbcCaUserAuthDao.getCaUserEntry(dn);
            if(rows.size() > 0){             
                if(rows.size() > 1) {
                    log.warn("User has more than a single CertificateRow entry rows: ["+rows.size()+"] dn: ["+dn+"]"); 
                }
                // if multiple cert rows were returned, then we should always 
                // use the row that has the latest expiry date ! 
                CertificateRow cr = rows.get(0); 
                Date max = rows.get(0).getNotAfter(); 
                for (CertificateRow testCr : rows) {
                    if(testCr.getNotAfter().after(max) ){
                      cr = testCr;   
                    }
                }
                
                List<GrantedAuthority> auths = new ArrayList<GrantedAuthority>();
                String role = this.getRoleFromDataColumn(cr); 
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
                return new CaUser(dn, true, true, true, true, auths, cr);
            }         
        } 
        throw new UsernameNotFoundException("User Not found [" + dn+ "]"); 
    }
    
    /**
     * Extract the value from the 'ROLE = role_value' serialized in data field of the given row.  
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
    public void setJdbcCaUserAuthDao(JdbcCaUserAuthDao jdbcCaUserAuthDao){
        this.jdbcCaUserAuthDao = jdbcCaUserAuthDao; 
    }

}
