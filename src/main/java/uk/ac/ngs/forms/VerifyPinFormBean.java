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
