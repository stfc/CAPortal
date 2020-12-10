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
import javax.validation.constraints.Pattern;

/**
 * Form submission bean for revoking a certificate.
 *
 * @author David Meredith
 */
public class RevokeCertFormBean {

    @Min(value = 0, message = "0 is minimum")
    private long cert_key;

    @Pattern(message = "Value required (Invalid chars \" ' ; `)", regexp = "^[^\"';`]+$")
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
