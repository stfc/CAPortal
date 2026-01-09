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
package uk.ac.ngs.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import uk.ac.ngs.SecurityConfiguration;
import uk.ac.ngs.dao.JdbcCaUserAuthDao;
import uk.ac.ngs.dao.JdbcCertificateDao;
import uk.ac.ngs.dao.JdbcCrrDao;
import uk.ac.ngs.dao.JdbcRalistDao;
import uk.ac.ngs.dao.JdbcRaopListDao;
import uk.ac.ngs.dao.JdbcRequestDao;
import uk.ac.ngs.dao.RoleChangeRequestRepository;
import uk.ac.ngs.domain.CertificateRow;
import uk.ac.ngs.domain.RoleChangeRequest;
import uk.ac.ngs.security.CaUser;
import uk.ac.ngs.security.SecurityContextService;
import uk.ac.ngs.service.CertificateService;
import uk.ac.ngs.service.email.EmailService;

@WebMvcTest(RaOpHome.class)
@Import(SecurityConfiguration.class)
public class RaopHomeTests {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CaUser caUser;

    @MockBean
    private JdbcCertificateDao jdbcCertificateDao;

    @MockBean
    private JdbcRalistDao ralistDao;

    @MockBean
    private JdbcRaopListDao jdbcRaopListDao;

    @MockBean
    private JdbcCrrDao jdbcCrrDao;

    @MockBean
    private JdbcRequestDao requestDao;

    @MockBean
    private JdbcCaUserAuthDao caUserAuthDao;

    @MockBean
    private CertificateService certificateService;

    @MockBean
    private SecurityContextService securityContextService;

    @MockBean
    private RoleChangeRequestRepository roleChangeRequestRepository;

    @MockBean
    private EmailService emailService;

    @MockBean
    private RedirectAttributes redirectAttributes;

    @Mock
    RaOpHome raOpHomeController;

    @Test
    @WithMockUser(roles = { "RAOP", "CAOP" })
    public void testApproveRoleChange_successfulRoleChange() throws Exception {
        long certKey = 123L;
        int requestId = 555;
        CertificateRow targetCert = getCert("Target User", "User");
        CertificateRow requesterCert = getCert("Requester", "RA Operator");
        RoleChangeRequest roleChangeRequest = new RoleChangeRequest();
        roleChangeRequest.setId(requestId);
        roleChangeRequest.setRequestedBy(requesterCert.getCert_key());

        CertificateRow currentUser = getCert("CAOP User", "CA Operator");

        when(jdbcCertificateDao.findById(certKey)).thenReturn(targetCert);
        when(roleChangeRequestRepository.findById(roleChangeRequest.getId()))
                .thenReturn(Optional.of(roleChangeRequest));
        when(jdbcCertificateDao.findById(requesterCert.getCert_key())).thenReturn(requesterCert);
        when(jdbcCertificateDao.findActiveCAs()).thenReturn(Arrays.asList(currentUser));

        when(securityContextService.getCaUserDetails()).thenReturn(caUser);
        when(caUser.getAuthorities()).thenReturn(Arrays.asList(new SimpleGrantedAuthority("ROLE_CAOP"),
                new SimpleGrantedAuthority("ROLE_RAOP")));
        when(caUser.getCertificateRow()).thenReturn(currentUser);

        mockMvc.perform(post("/raop/approverolechange")
                .with(csrf())
                .param("certKey", String.valueOf(certKey))
                .param("requestId", String.valueOf(requestId)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/raop"))
                .andExpect(flash().attribute("responseMessage", "Role updated successfully!"));

        verify(certificateService).updateCertificateRole(certKey, "RA Operator");
        verify(emailService, times(3)).sendEmailOnRaopRoleRequestApproval(any(), any(), any(), any());
    }

    private CertificateRow getCert(String userName, String role) {
        int certKey = ThreadLocalRandom.current().nextInt(100, 1000);
        CertificateRow cert = new CertificateRow();
        cert.setCert_key(certKey);
        cert.setDn("CN=" + userName + ",OU=xyz_c,O=stfc.com,L=OX,ST=OX,C=UK");
        cert.setEmail("user" + certKey + "@example.com");
        cert.setRole(role);
        cert.setData("ROLE=" + role + "\r\nPROFILE=UKPERSON");
        return cert;
    }

    @Test
    @WithMockUser(roles = "RAOP")
    public void testApproveRoleChange_permissionDeniedForRAOP() throws Exception {
        performPermissionDeniedTest("RAOP User", "RA Operator");
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testApproveRoleChange_permissionDeniedForUser() throws Exception {
        performPermissionDeniedTest("Test User", "User");
    }

    private void performPermissionDeniedTest(String userName, String userRole) throws Exception {
        long certKey = 123L;
        int requestId = 555;
        CertificateRow currentUser = getCert(userName, userRole);

        when(securityContextService.getCaUserDetails()).thenReturn(caUser);
        when(caUser.getCertificateRow()).thenReturn(currentUser);

        mockMvc.perform(post("/raop/approverolechange")
                .with(csrf())
                .param("certKey", String.valueOf(certKey))
                .param("requestId", String.valueOf(requestId)))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/WEB-INF/views//denied/denied.jsp"))
                .andExpect(result -> {
                    Throwable resolvedException = result.getResolvedException();
                    assert resolvedException instanceof AccessDeniedException;
                });

        verify(certificateService, never()).updateCertificateRole(anyLong(), anyString());
        verify(emailService, never()).sendEmailOnRaopRoleRequestApproval(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = { "RAOP", "CAOP" })
    public void testRejectRoleChange_successful() throws Exception {
        long certKey = 123L;
        int requestId = 555;
        CertificateRow targetCert = getCert("Target User", "User");
        CertificateRow requesterCert = getCert("Requester", "RA Operator");
        RoleChangeRequest roleChangeRequest = new RoleChangeRequest();
        roleChangeRequest.setId(requestId);
        roleChangeRequest.setRequestedBy(requesterCert.getCert_key());

        CertificateRow currentUser = getCert("CAOP User", "CA Operator");

        when(jdbcCertificateDao.findById(certKey)).thenReturn(targetCert);
        when(roleChangeRequestRepository.findById(roleChangeRequest.getId()))
                .thenReturn(Optional.of(roleChangeRequest));
        when(jdbcCertificateDao.findById(requesterCert.getCert_key())).thenReturn(requesterCert);
        when(jdbcCertificateDao.findActiveCAs()).thenReturn(Arrays.asList(currentUser));

        when(securityContextService.getCaUserDetails()).thenReturn(caUser);
        when(caUser.getAuthorities()).thenReturn(Arrays.asList(new SimpleGrantedAuthority("ROLE_CAOP"),
                new SimpleGrantedAuthority("ROLE_RAOP")));
        when(caUser.getCertificateRow()).thenReturn(currentUser);

        mockMvc.perform(post("/raop/rejectrolechange")
                .with(csrf())
                .param("certKey", String.valueOf(certKey))
                .param("requestId", String.valueOf(requestId)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/raop"))
                .andExpect(flash().attribute("responseMessage", "Request rejected successfully!"));

        verify(roleChangeRequestRepository).deleteById(requestId);
        verify(emailService, times(3)).sendEmailOnRaopRoleRequestRejection(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "RAOP")
    public void testRejectRoleChange_permissionDeniedForRAOP() throws Exception {
        performPermissionDeniedTestForRejectRoleChange("RAOP User", "RA Operator");
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testRejectRoleChange_permissionDeniedForUser() throws Exception {
        performPermissionDeniedTestForRejectRoleChange("Test User", "User");
    }

    private void performPermissionDeniedTestForRejectRoleChange(String userName, String userRole) throws Exception {
        long certKey = 123L;
        int requestId = 555;
        CertificateRow currentUser = getCert(userName, userRole);

        when(securityContextService.getCaUserDetails()).thenReturn(caUser);
        when(caUser.getCertificateRow()).thenReturn(currentUser);

        mockMvc.perform(post("/raop/rejectrolechange")
                .with(csrf())
                .param("certKey", String.valueOf(certKey))
                .param("requestId", String.valueOf(requestId)))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/WEB-INF/views//denied/denied.jsp"))
                .andExpect(result -> {
                    Throwable resolvedException = result.getResolvedException();
                    assert resolvedException instanceof AccessDeniedException;
                });

        verify(roleChangeRequestRepository, never()).deleteById(requestId);
        verify(emailService, never()).sendEmailOnRaopRoleRequestRejection(any(), any(), any(), any());
    }
}
