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

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDate;

@Table("role_change_request")
public class RoleChangeRequest {

    @Id
    private Integer id;

    @Column("cert_key")
    private Long certKey;

    @Column("requested_role")
    private String requestedRole;

    @Column("requested_by")
    private Long requestedBy;

    @Column("requested_on")
    private LocalDate requestedOn;

    // Constructors
    public RoleChangeRequest() {}

    public RoleChangeRequest(Long certKey, String requestedRole, Long requestedBy, LocalDate requestedOn) {
        this.certKey = certKey;
        this.requestedRole = requestedRole;
        this.requestedBy = requestedBy;
        this.requestedOn = requestedOn;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getCertKey() {
        return certKey;
    }

    public void setCertKey(Long certKey) {
        this.certKey = certKey;
    }

    public String getRequestedRole() {
        return requestedRole;
    }

    public void setRequestedRole(String requestedRole) {
        this.requestedRole = requestedRole;
    }

    public Long getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(Long requestedBy) {
        this.requestedBy = requestedBy;
    }

    public LocalDate getRequestedOn() {
        return requestedOn;
    }

    public void setRequestedOn(LocalDate requestedOn) {
        this.requestedOn = requestedOn;
    }
}

