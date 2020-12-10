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

import javax.validation.constraints.Min;
import java.io.Serializable;

//import javax.validation.constraints.Max;
//import javax.validation.constraints.NotNull;
//import javax.validation.constraints.Pattern;


/**
 * Simple form submission bean for submitting a page number.
 *
 * @author David Meredith
 */
public class GotoPageNumberFormBean implements Serializable {


    @Min(value = 0, message = "0 is minimum")
    private Integer gotoPageNumber = 0;

    /**
     * @return the pageNumber
     */
    public Integer getGotoPageNumber() {
        return gotoPageNumber;
    }

    /**
     * @param pageNumber the pageNumber to set
     */
    public void setGotoPageNumber(Integer gotoPageNumber) {
        this.gotoPageNumber = gotoPageNumber;
    }


}
