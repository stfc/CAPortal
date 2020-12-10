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

import java.io.Serializable;
import java.util.Date;


/**
 * Domain object that maps to the <code>certificate</code> DB table.
 *
 * @author David Meredith
 */
public class CertificateRow implements Serializable {

    private static final long serialVersionUID = -300138607282104550L;

    private long cert_key;
    private String data;
    private String dn;
    private String cn;
    private String email;
    private String status;
    private String role;
    private Date notAfter;

    public String getCn() {
        return cn;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }

    public long getCert_key() {
        return cert_key;
    }

    public void setCert_key(long cert_key) {
        this.cert_key = cert_key;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getDn() {
        return dn;
    }

    public Date getNotAfter() {
        return this.notAfter;
    }

    public void setNotAfter(Date notAfter) {
        this.notAfter = notAfter;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }


}
