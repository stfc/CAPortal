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

package uk.ac.ngs.domain;

import uk.ac.ngs.common.CertUtil;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transfer bean for use in view layer.
 * This bean adds extra properties such as the checked that are not required
 * by the domain model, but are required by the view layer.
 *
 * @author Sam Worley
 * @author David Meredith
 */
public class RequestWrapper {
    private final static Pattern DATA_CERT_PATTERN = Pattern.compile("-----BEGIN CERTIFICATE REQUEST-----(.+?)-----END CERTIFICATE REQUEST-----", Pattern.DOTALL);
    private boolean checked;
    private RequestRow requestRow;

    /**
     * Create a new instance
     *
     * @param checked
     * @param requestRow
     */
    public RequestWrapper(boolean checked, RequestRow requestRow) {
        this.checked = checked;
        this.requestRow = requestRow;
    }

    /**
     * Default constructor - required so Spring can auto-create/bind instances in view
     */
    public RequestWrapper() {
    }

    /**
     * @return the checked
     */
    public boolean isChecked() {
        return checked;
    }

    /**
     * @return the checked
     */
    public boolean getChecked() {
        return checked;
    }

    /**
     * Set the checked value
     *
     * @param checked
     */
    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    /**
     * @return the requestRow
     */
    public RequestRow getRequestRow() {
        return requestRow;
    }

    /**
     * Set the RequestRow
     *
     * @param requestRow
     */
    public void setRequestRow(RequestRow requestRow) {
        this.requestRow = requestRow;
    }

    /**
     *
     * @return whether a certificate in the row has SANs or not
     */
    public boolean hasSans() {
        Matcher certmatcher = DATA_CERT_PATTERN.matcher(requestRow.getData());
        ArrayList<String> sans = CertUtil.getSansCSR(certmatcher, this.requestRow);
        if(sans != null) {
            return sans.size() > 1;
        }
        return false;
    }
}
