package uk.ac.ngs.controllers;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

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
import uk.ac.ngs.dao.JdbcRalistDao;
import uk.ac.ngs.dao.JdbcRequestDao;
import uk.ac.ngs.dao.RoleChangeRequestRepository;
import uk.ac.ngs.domain.CertificateRow;
import uk.ac.ngs.security.CaUser;
import uk.ac.ngs.security.SecurityContextService;
import uk.ac.ngs.service.CertificateService;
import uk.ac.ngs.service.email.EmailService;

import org.junit.jupiter.api.Test;

@WebMvcTest(ManageRaop.class)
@Import(SecurityConfiguration.class)
public class ManageRaopTests {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CaUser caUser;

    @MockBean
    private JdbcRalistDao ralistDao;

    @MockBean
    private JdbcRequestDao requestDao;

    @MockBean
    private JdbcCaUserAuthDao caUserAuthDao;

    @MockBean
    private JdbcCertificateDao jdbcCertificateDao;

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
    ManageRaop manageRaopController;

    @Test
    @WithMockUser(roles = "RAOP")
    public void testChangeRoleToUser_successfulRoleChange() throws Exception {
        long certKey = 123L;
        CertificateRow targetCert = getTargetCert();

        CertificateRow currentUser = new CertificateRow();
        currentUser.setDn("CN=Test User,OU=xyz_c,O=stfc.com,L=OX,ST=OX,C=UK");

        when(jdbcCertificateDao.findById(certKey)).thenReturn(targetCert);
        when(securityContextService.getCaUserDetails()).thenReturn(caUser);
        when(caUser.getAuthorities()).thenReturn(Arrays.asList(new SimpleGrantedAuthority("ROLE_RAOP")));
        when(caUser.getCertificateRow()).thenReturn(currentUser);

        mockMvc.perform(post("/raop/manageraop/changeroletouser")
                        .with(csrf())
                        .param("cert_key", String.valueOf(certKey)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/raop/manageraop"))
                .andExpect(flash().attribute("responseMessage", "Role updated successfully!"));

        verify(certificateService).updateCertificateRole(certKey, "User");
    }

    private CertificateRow getTargetCert() {
        CertificateRow targetCert = new CertificateRow();
        targetCert.setDn("CN=Target User,OU=xyz_c,O=stfc.com,L=OX,ST=OX,C=UK");
        targetCert.setRole("RA Operator");
        targetCert.setData("ROLE=RA Operator\r\nPROFILE=UKPERSON");
        return targetCert;
    }

    @Test
    @WithMockUser(roles = "RAOP")
    public void testChangeRoleToUser_permissionDenied() throws Exception {
        long certKey = 123L;
        CertificateRow targetCert = new CertificateRow();
        targetCert.setRole("RA Operator");

        CertificateRow currentUser = new CertificateRow();
        currentUser.setDn("CN=Test User");

        when(jdbcCertificateDao.findById(certKey)).thenReturn(targetCert);
        when(securityContextService.getCaUserDetails()).thenReturn(caUser);
        when(caUser.getCertificateRow()).thenReturn(currentUser);

        mockMvc.perform(post("/raop/manageraop/changeroletouser")
                        .with(csrf())
                        .param("cert_key", String.valueOf(certKey)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/raop/manageraop"))
                // Role Change FAIL as caUser.getAuthorities() not mocked and does not have RAOP role
                .andExpect(flash().attribute("responseMessage", "Role Change FAIL - user does not have correct permissions"));

        verify(certificateService, never()).updateCertificateRole(anyLong(), anyString());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testChangeRoleToUser_permissionDeniedForUser() throws Exception {
        long certKey = 123L;
        CertificateRow targetCert = new CertificateRow();
        targetCert.setRole("RA Operator");

        CertificateRow currentUser = new CertificateRow();
        currentUser.setDn("CN=Test User");

        when(jdbcCertificateDao.findById(certKey)).thenReturn(targetCert);
        when(securityContextService.getCaUserDetails()).thenReturn(caUser);
        when(caUser.getCertificateRow()).thenReturn(currentUser);

        mockMvc.perform(post("/raop/manageraop/changeroletouser")
                        .with(csrf())
                        .param("cert_key", String.valueOf(certKey)))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/WEB-INF/views//denied/denied.jsp"))
                .andExpect(result -> {
                    Throwable resolvedException = result.getResolvedException();
                    assert resolvedException instanceof AccessDeniedException;
                });

        verify(certificateService, never()).updateCertificateRole(anyLong(), anyString());
    }

    @Test
    @WithMockUser(roles = "RAOP")
    void testAccessWithRAOPRole_shouldSucceed() throws Exception {
        long certKey = 123L;
        
        CertificateRow targetCert = new CertificateRow();
        targetCert.setRole("RA Operator");

        CertificateRow currentUser = new CertificateRow();
        currentUser.setDn("CN=Test User");

        when(jdbcCertificateDao.findById(certKey)).thenReturn(targetCert);
        when(securityContextService.getCaUserDetails()).thenReturn(caUser);
        when(caUser.getCertificateRow()).thenReturn(currentUser);

        mockMvc.perform(get("/raop/manageraop"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testAccessWithUserRole_shouldBeDenied() throws Exception {
        
        mockMvc.perform(get("/raop/manageraop"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/WEB-INF/views//denied/denied.jsp"))
                .andExpect(result -> {
                    Throwable resolvedException = result.getResolvedException();
                    assert resolvedException instanceof AccessDeniedException;
                });

    }
}
