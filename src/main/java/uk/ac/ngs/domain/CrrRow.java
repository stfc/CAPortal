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

import java.util.Date;

/**
 * Domain object for the <code>crr</code> table.
 *
 * @author David Meredith
 */
public class CrrRow {

    private long crr_key;
    private long cert_key;
    private String submit_date;
    private String format;
    private String data;
    private String dn;
    private String cn;
    private String email;
    private String ra;
    private String rao;
    private String status;
    private String reason;
    private String loa;

    // Additional attributes that are not db columns. 
    private Date dataSubmit_Date;

    public long getCrr_key() {
        return crr_key;
    }

    public void setCrr_key(long crr_key) {
        this.crr_key = crr_key;
    }

    public long getCert_key() {
        return cert_key;
    }

    public void setCert_key(long cert_key) {
        this.cert_key = cert_key;
    }

    public String getSubmit_date() {
        return submit_date;
    }

    public void setSubmit_date(String submit_date) {
        this.submit_date = submit_date;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
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

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getCn() {
        return cn;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRa() {
        return ra;
    }

    public void setRa(String ra) {
        this.ra = ra;
    }

    public String getRao() {
        return rao;
    }

    public void setRao(String rao) {
        this.rao = rao;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getLoa() {
        return loa;
    }

    public void setLoa(String loa) {
        this.loa = loa;
    }

    /**
     * @return the dataSubmit_Date
     */
    public Date getDataSubmit_Date() {
        return dataSubmit_Date;
    }

    /**
     * @param dataSubmit_Date the dataSubmit_Date to set
     */
    public void setDataSubmit_Date(Date dataSubmit_Date) {
        this.dataSubmit_Date = dataSubmit_Date;
    }


}
