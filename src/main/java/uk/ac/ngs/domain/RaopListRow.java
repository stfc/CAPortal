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
 * Domain object for the <code>raoplist</code> table.
 *
 * @author David Meredith
 */
public class RaopListRow {

    private String ou;
    private String l;
    private String name;
    private String email;
    private String phone;
    private String street;
    private String city;
    private String postcode;
    private String cn;
    private String title;
    private String coneemail;
    private String location;
    private Boolean manager; // BIT 
    private Boolean operator; // BIT
    private Integer ra_id; // int 
    private Integer ra_id2; // int 
    private Date trainingDate;
    private String department_hp;
    private String institute_hp;
    private Boolean active; // BIT

    public String getOu() {
        return ou;
    }

    public void setOu(String ou) {
        this.ou = ou;
    }

    public String getL() {
        return l;
    }

    public void setL(String l) {
        this.l = l;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    public String getCn() {
        return cn;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getConeemail() {
        return coneemail;
    }

    public void setConeemail(String coneemail) {
        this.coneemail = coneemail;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Boolean getManager() {
        return manager;
    }

    public void setManager(Boolean manager) {
        this.manager = manager;
    }

    public Boolean getOperator() {
        return operator;
    }

    public void setOperator(Boolean operator) {
        this.operator = operator;
    }

    public Integer getRa_id() {
        return ra_id;
    }

    public void setRa_id(Integer ra_id) {
        this.ra_id = ra_id;
    }

    public Date getTrainingDate() {
        return trainingDate;
    }

    public void setTrainingDate(Date trainingDate) {
        this.trainingDate = trainingDate;
    }

    public String getDepartment_hp() {
        return department_hp;
    }

    public void setDepartment_hp(String department_hp) {
        this.department_hp = department_hp;
    }

    public String getInstitute_hp() {
        return institute_hp;
    }

    public void setInstitute_hp(String institute_hp) {
        this.institute_hp = institute_hp;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    /**
     * @return the ra_id2
     */
    public Integer getRa_id2() {
        return ra_id2;
    }

    /**
     * @param ra_id2 the ra_id2 to set
     */
    public void setRa_id2(Integer ra_id2) {
        this.ra_id2 = ra_id2;
    }


}
