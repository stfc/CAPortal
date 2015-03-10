/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.ngs.forms;

import java.io.Serializable;
import javax.validation.constraints.Size;
//import javax.validation.constraints.Pattern;

/**
 * Form bean for verifyPin page. 
 * @author David Meredith  
 */
public class VerifyPinFormBean implements Serializable {

    //@Pattern(message="Value required (Invalid chars \" ' ; `)", regexp="^[^\"';`]+$") 
    @Size(message="Value required", min=2, max=240)
    private String pinVerification;

    //@Pattern(message="Value required (Invalid chars \" ' ; `)", regexp="^[^\"';`]+$") 
    @Size(message="Value required", min=2, max=240)
    private String pin; 

    /**
     * @return the pinVerification
     */
    public String getPinVerification() {
        return pinVerification;
    }

    /**
     * @param pinVerification the pinVerification to set
     */
    public void setPinVerification(String pinVerification) {
        this.pinVerification = pinVerification;
    }

    /**
     * @return the pin
     */
    public String getPin() {
        return pin;
    }

    /**
     * @param pin the pin to set
     */
    public void setPin(String pin) {
        this.pin = pin;
    }

    

   
    
    
}
