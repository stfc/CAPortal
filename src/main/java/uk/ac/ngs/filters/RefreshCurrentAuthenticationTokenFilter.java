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
package uk.ac.ngs.filters;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.ac.ngs.security.SecurityContextService;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Called on every request that is intercepted by Spring Security to
 * refresh the user's current security context (if any), including their
 * current Authentication token and their granted authorities (roles).
 * <p>
 * If there is no current security context for the user, e.g. the user has not yet
 * authenticated and they are viewing pages with a 'permitAll' access declaration,
 * then the filter does nothing and continues with the filter chain.
 * <p>
 * This filter should be called prior to any authorization decision is taken
 * (usually before the filter_security_interceptor that will allow/deny access
 * to the resource based on the updated roles/authentication context).
 *
 * @author David Meredith
 */
public class RefreshCurrentAuthenticationTokenFilter extends OncePerRequestFilter {
    private static final Log log = LogFactory.getLog(RefreshCurrentAuthenticationTokenFilter.class);
    private SecurityContextService securityContextService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Roles are stored in the users session so update these roles on every 
        // request. This is necessary as the DB state may have changed (e.g. 
        // user cert is revoked) which would cause a subsequent update in their roles.  
        //log.debug("doFilterInternal"); 
        this.securityContextService.refreshCurrentAuthenticationToken();
        filterChain.doFilter(request, response);
    }

    @Inject
    public void setSecurityContextService(SecurityContextService securityContextService) {
        this.securityContextService = securityContextService;
    }

}
