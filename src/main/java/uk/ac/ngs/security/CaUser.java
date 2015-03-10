package uk.ac.ngs.security;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import uk.ac.ngs.domain.CertificateRow;

/**
 * Custom UserDetails so we can decorate as required. 
 * @author David Meredith 
 */
public class CaUser extends User {

    private static final long serialVersionUID = -5805271694653223569L;
    private final CertificateRow certRow; 
    
    /**
     * No password is required since we use certificate DNs for auth. 
     * @see org.springframework.security.core.userdetails.User#User(String, String, boolean, boolean, boolean, boolean, Collection) 
     */
    public CaUser(String username, boolean enabled,
            boolean accountNonExpired, boolean credentialsNonExpired,
            boolean accountNonLocked,
            Collection<? extends GrantedAuthority> authorities, CertificateRow cr) {
        super(username, "", enabled, accountNonExpired, credentialsNonExpired,
                accountNonLocked, authorities);
        this.certRow = cr; 
    }
    
    public CertificateRow getCertificateRow(){
        return this.certRow; 
    }

}
