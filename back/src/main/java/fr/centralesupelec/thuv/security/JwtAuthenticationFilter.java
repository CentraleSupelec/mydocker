package fr.centralesupelec.thuv.security;

import fr.centralesupelec.thuv.model.Role;
import fr.centralesupelec.thuv.security.dtos.TokenOrigin;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String AUTHORIZATION_HEADER = "Bearer ";
    private static final Set<String> PRIVILEGED_AUTHORITIES = Set.of("ROLE_ADMIN");
    private static final Set<String> PRIVILEGED_ROLES = Set.of("ROLE_ADMIN");

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserDetailsService userDetailsService;

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private SecurityContextRepository securityContextRepository;

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.getUsernameFromJWT(jwt);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (!userDetails.isEnabled()) {
                    throw new DisabledException("Account disabled");
                }
                String origin = tokenProvider.getOriginFromJWT(jwt);
                UserDetails principal = effectivePrincipal(userDetails, origin, username);
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        authoritiesFor(principal, origin, username)
                );
                SecurityContext securityContext = SecurityContextHolder.getContext();
                securityContext.setAuthentication(authentication);
                securityContextRepository.saveContext(securityContext, request, response);
            }
        } catch (DisabledException ex) {
            logger.warn("Disabled user attempted to authenticate");
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Account disabled");
            return;
        } catch (Exception ex) {
            logger.error("Could not authenticate user", ex);
            Sentry.captureException(ex);
        }
        filterChain.doFilter(request, response);
    }

    /**
     * The principal, not only the Authentication, has to carry the effective roles: permission
     * services such as CoursePermissionService and CourseUpdateService read
     * principal.getAuthorities() rather than the authentication's, so filtering one and not the
     * other would leave administrator decisions reachable from a session that must not have them.
     *
     * A new MyUserDetails is built; the role collection it was given belongs to the user entity and
     * is never mutated.
     */
    private UserDetails effectivePrincipal(UserDetails userDetails, String origin, String username) {
        if (TokenOrigin.grantsPrivilegedAuthority(origin) || !(userDetails instanceof MyUserDetails details)) {
            return userDetails;
        }
        List<Role> retained = details
                .getRoles()
                .stream()
                .filter(role -> !PRIVILEGED_ROLES.contains(role.getName()))
                .collect(Collectors.toList());
        if (retained.size() == details.getRoles().size()) {
            return userDetails;
        }
        logger.warn("Dropped privileged roles for {}: token origin '{}' does not grant them", username, origin);
        return new MyUserDetails(retained, details.getEmail(), details.getUserId(), details.getEnabled());
    }

    /**
     * Roles are re-read from the database on every request, so a token minted before an account was
     * promoted would otherwise carry the new authority. Belt and braces for any principal that is
     * not a MyUserDetails: a new list is built rather than the one the UserDetails hands over.
     */
    private Collection<? extends GrantedAuthority> authoritiesFor(
            UserDetails userDetails, String origin, String username
    ) {
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        if (TokenOrigin.grantsPrivilegedAuthority(origin)) {
            return authorities;
        }
        List<GrantedAuthority> filtered = authorities
                .stream()
                .filter(authority -> !PRIVILEGED_AUTHORITIES.contains(authority.getAuthority()))
                .collect(Collectors.toList());
        if (filtered.size() != authorities.size()) {
            logger.warn("Dropped privileged authorities for {}: token origin '{}' does not grant them", username, origin);
        }
        return filtered;
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(AUTHORIZATION_HEADER)) {
            return bearerToken.substring(AUTHORIZATION_HEADER.length());
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getServletPath().startsWith("/container/");
    }
}
