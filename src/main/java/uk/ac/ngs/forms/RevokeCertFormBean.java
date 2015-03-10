/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.ngs.forms;

import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

/**
 * Form submission bean for revoking a certificate. 
 * @author David Meredith  
 */
public class RevokeCertFormBean {

    @Min(value=0, message="0 is minimum")
    private long cert_key; 

    @Pattern(message="Value required (Invalid chars \" ' ; `)", regexp="^[^\"';`]+$") 
    private String reason; 

    /**
     * @return the cert_key
     */
    public long getCert_key() {
        return cert_key;
    }

    /**
     * @param cert_key the cert_key to set
     */
    public void setCert_key(long cert_key) {
        this.cert_key = cert_key;
    }

    /**
     * @return the reason
     */
    public String getReason() {
        return reason;
    }

    /**
     * @param reason the reason to set
     */
    public void setReason(String reason) {
        this.reason = reason;
    }

    
    
}
