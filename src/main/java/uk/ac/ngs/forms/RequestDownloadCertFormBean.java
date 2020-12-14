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

import javax.validation.constraints.*;
import java.io.Serializable;

/**
 * Form bean for downloading certificates.
 *
 * @author David Meredith
 */
public class RequestDownloadCertFormBean implements Serializable {
    // http://codetutr.com/2013/05/28/spring-mvc-form-validation/

    @NotNull
    @Min(1)
    @Max(Long.MAX_VALUE)
    private Long certId;

    @NotNull
    @NotEmpty
    @Email
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
