package fr.centralesupelec.thuv.security;

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
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        authoritiesFor(userDetails, tokenProvider.getOriginFromJWT(jwt), username)
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
     * Roles are re-read from the database on every request, so a token minted before an account was
     * promoted would otherwise carry the new authority. The origin the token was issued from decides
     * whether privileged authority applies at all, and a new list is built rather than the one the
     * UserDetails hands over, which is backed by the user entity.
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
