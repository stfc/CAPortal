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
 * Form bean for inputting a single string value. Can be used in a number of
 * form POSTs that require just a single string.
 *
 * @author David Meredith
 */
public class SingleStringFormBean implements Serializable {

    @Pattern(message = "Invalid chars \" ' ; `", regexp = "^[^\"';`]*$")
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
