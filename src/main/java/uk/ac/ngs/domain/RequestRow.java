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
 * Domain object for the <code>request</code> table. 
 * @author David Meredith 
 *
 */
public class RequestRow {

    private long req_key; 
    private String format; 
    private String data; 
    private String dn; 
    private String cn; 
    private String email; 
    private String ra; 
    private String rao; 
    private String status; 
    private String role; 
    private String public_key;
    private String scep_tid; 
    private String loa; 
    private Long bulk = null;
    //private boolean exported;

    // Additional attributes that are not db columns. 
    private Date dataNotBefore; 
   
    
    public long getReq_key() {
        return req_key;
    }
    public void setReq_key(long req_key) {
        this.req_key = req_key;
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
    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }
    public String getPublic_key() {
        return public_key;
    }
    public void setPublic_key(String public_key) {
        this.public_key = public_key;
    }
    public String getScep_tid() {
        return scep_tid;
    }
    public void setScep_tid(String scep_tid) {
        this.scep_tid = scep_tid;
    }
    public String getLoa() {
        return loa;
    }
    public void setLoa(String loa) {
        this.loa = loa;
    }
    public Long getBulk() {
        return bulk;
    }
    public void setBulk(Long bulk) {
        this.bulk = bulk;
    } 
    /*public boolean getExported(){
        return this.exported;
    }
    public void setExported(boolean exported){
        this.exported = exported; 
    }*/

    /**
     * @return the dataNotBefore
     */
    public Date getDataNotBefore() {
        return dataNotBefore;
    }

    /**
     * @param dataNotBefore the dataNotBefore to set
     */
    public void setDataNotBefore(Date dataNotBefore) {
        this.dataNotBefore = dataNotBefore;
    }
    
    
}
