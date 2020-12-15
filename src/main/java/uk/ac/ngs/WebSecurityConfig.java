package uk.ac.ngs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.ac.ngs.filters.RefreshCurrentAuthenticationTokenFilter;
import uk.ac.ngs.security.CaJdbcUserDetailsService;
import uk.ac.ngs.security.CaX509JdbcPreAuthUserDetails;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(jsr250Enabled = true, securedEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.addFilterBefore(refreshAuthenticationTokenFilter(), FilterSecurityInterceptor.class)
                .x509().authenticationUserDetailsService(caX590AuthMan())
                .and().csrf().disable()
                .exceptionHandling().accessDeniedPage("/denied").authenticationEntryPoint(authenticationEntryPoint());
    }

    @Bean
    public OncePerRequestFilter refreshAuthenticationTokenFilter() {
        return new RefreshCurrentAuthenticationTokenFilter();
    }

    @Bean
    public UserDetailsService caJdbcUserDetailsService() {
        return new CaJdbcUserDetailsService();
    }

    @Bean AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> caX590AuthMan() {
        return new CaX509JdbcPreAuthUserDetails();
    }

    @Bean
    public LoginUrlAuthenticationEntryPoint authenticationEntryPoint() {
        return new LoginUrlAuthenticationEntryPoint("/denied");
    }
}
