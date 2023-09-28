package uk.ac.ngs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.security.web.session.SessionManagementFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.ac.ngs.filters.RefreshCurrentAuthenticationTokenFilter;
import uk.ac.ngs.security.CaJdbcUserDetailsService;
import uk.ac.ngs.security.CaX509JdbcPreAuthUserDetails;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true, securedEnabled = true)
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.addFilterAfter(refreshAuthenticationTokenFilter(), SessionManagementFilter.class)
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers(new AntPathRequestMatcher("/"), new AntPathRequestMatcher("/WEB-INF/views/home.jsp"), new AntPathRequestMatcher("/resources/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/denied"), new AntPathRequestMatcher("/WEB-INF/views/denied/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/pub/**"), new AntPathRequestMatcher("/WEB-INF/views/pub/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/caop/**"), new AntPathRequestMatcher("/WEB-INF/views/caop/**")).authenticated()
                        .requestMatchers(new AntPathRequestMatcher("/raop/**"), new AntPathRequestMatcher("/WEB-INF/views/raop/**")).authenticated()
                        .requestMatchers(new AntPathRequestMatcher("/cert_owner/**"), new AntPathRequestMatcher("/WEB-INF/views/cert_owner/**")).authenticated()
                )
                .x509((customizer) -> customizer.authenticationUserDetailsService(caX590AuthMan()))
                .exceptionHandling((customizer) ->
                        customizer.accessDeniedPage("/denied")
                                .authenticationEntryPoint(authenticationEntryPoint()));
        return http.build();
    }

    @Bean
    public OncePerRequestFilter refreshAuthenticationTokenFilter() {
        return new RefreshCurrentAuthenticationTokenFilter();
    }

    @Bean
    public UserDetailsService caJdbcUserDetailsService() {
        return new CaJdbcUserDetailsService();
    }

    @Bean
    AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> caX590AuthMan() {
        return new CaX509JdbcPreAuthUserDetails();
    }

    @Bean
    public LoginUrlAuthenticationEntryPoint authenticationEntryPoint() {
        return new LoginUrlAuthenticationEntryPoint("/denied");
    }
}
