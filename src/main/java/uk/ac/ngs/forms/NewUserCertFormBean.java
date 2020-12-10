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

import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * @author David Meredith
 */
public class NewUserCertFormBean implements Serializable {

    @Pattern(message = "Invalid chars \" ' ; `", regexp = "^[^\"';`]*$")
    private String ra;

    @Pattern(message = "Invalid name (at least 2 names min of 2 chars each)", regexp = "^\\w{2,30}( \\w{2,30})+$")
    private String name;

    @Pattern(message = "Invalid email", regexp = "^(([0-9a-zA-Z]+[-._])*[0-9a-zA-Z]+@([-0-9a-zA-Z]+[.])+[a-zA-Z]{2,6}[,;]?)+$")
    private String emailAddress;

    @Pattern(message = "Invalid PIN (10 chars min)", regexp = "^[0-9a-zA-z ]{10,20}$")
    private String pin;

    @Pattern(message = "Invalid Password (10 chars min, invalid chars \"';)", regexp = "^[^'\";]{10,50}$")
    private String password;


    /**
     * @return the ra
     */
    public String getRa() {
        return ra;
    }

    /**
     * @param ra the ra to set
     */
    public void setRa(String ra) {
        this.ra = ra;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the emailAddress
     */
    public String getEmailAddress() {
        return emailAddress;
    }

    /**
     * @param emailAddress the emailAddress to set
     */
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
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

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the pw to set
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
