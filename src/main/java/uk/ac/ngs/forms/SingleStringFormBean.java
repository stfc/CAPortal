/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.ngs.forms;

import java.io.Serializable;
import javax.validation.constraints.Pattern;

/**
 * Form bean for inputting a single string value. Can be used in a number of 
 * form POSTs that require just a single string. 
 * @author David Meredith  
 */
public class SingleStringFormBean implements Serializable {

    @Pattern(message="Invalid chars \" ' ; `", regexp="^[^\"';`]*$") 
    private String value; 

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }
    
    
}
