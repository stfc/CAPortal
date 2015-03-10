/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.ngs.forms;

import java.io.Serializable;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Form bean for downloading certificates. 
 * @author David Meredith  
 */
public class RequestDownloadCertFormBean implements Serializable{
    // http://codetutr.com/2013/05/28/spring-mvc-form-validation/
    
    @NotNull
    @Min(1) @Max(Long.MAX_VALUE)
    private Long certId; 

    @NotNull
    @NotEmpty @Email
    private String email; 



    /**
     * @return the certificate id  
     */
    public Long getCertId() {
        return certId;
    }

    /**
     * @param certId the certificate Id to set
     */
    public void setCertId(Long certId) {
        this.certId = certId;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    
    
}
