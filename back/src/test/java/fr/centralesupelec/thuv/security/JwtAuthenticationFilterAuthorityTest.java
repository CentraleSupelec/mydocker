package fr.centralesupelec.thuv.security;

import fr.centralesupelec.thuv.model.Role;
import fr.centralesupelec.thuv.security.dtos.TokenOrigin;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Authorities are re-read from the database on every request, so the origin a token was issued from
 * is what decides whether privileged authority applies - not the role the account happens to hold
 * by the time the token is used.
 */
class JwtAuthenticationFilterAuthorityTest {
    private static final String TOKEN = "a.b.c";

    private JwtTokenProvider tokenProvider;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        tokenProvider = mock(JwtTokenProvider.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);

        Role admin = new Role();
        admin.setName("ROLE_ADMIN");
        Role user = new Role();
        user.setName("ROLE_USER");
        MyUserDetails userDetails = new MyUserDetails(List.of(admin, user), "someone", 1L, true);

        when(tokenProvider.validateToken(TOKEN)).thenReturn(true);
        when(tokenProvider.getUsernameFromJWT(TOKEN)).thenReturn("someone");
        when(userDetailsService.loadUserByUsername("someone")).thenReturn(userDetails);

        filter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "tokenProvider", tokenProvider);
        ReflectionTestUtils.setField(filter, "userDetailsService", userDetailsService);
        ReflectionTestUtils.setField(filter, "securityContextRepository", mock(SecurityContextRepository.class));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Set<String> authoritiesForOrigin(String origin) throws Exception {
        when(tokenProvider.getOriginFromJWT(TOKEN)).thenReturn(origin);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + TOKEN);
        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    @Test
    void ourOwnProvidersKeepPrivilegedAuthority() throws Exception {
        assertTrue(authoritiesForOrigin(TokenOrigin.CAS.toString()).contains("ROLE_ADMIN"));
        SecurityContextHolder.clearContext();
        assertTrue(authoritiesForOrigin(TokenOrigin.OIDC.toString()).contains("ROLE_ADMIN"));
    }

    @Test
    void federatedOriginsLosePrivilegedAuthorityButKeepTheRest() throws Exception {
        Set<String> authorities = authoritiesForOrigin(TokenOrigin.LTI.toString());
        assertFalse(authorities.contains("ROLE_ADMIN"));
        assertEquals(Set.of("ROLE_USER"), authorities);
    }

    @Test
    void magicLinkLosesPrivilegedAuthority() throws Exception {
        assertFalse(authoritiesForOrigin(TokenOrigin.MAGIC_LINK.toString()).contains("ROLE_ADMIN"));
    }

    @Test
    void aMissingOriginClaimFailsClosed() throws Exception {
        assertFalse(authoritiesForOrigin(null).contains("ROLE_ADMIN"));
    }

    @Test
    void anUnrecognisedOriginFailsClosed() throws Exception {
        assertFalse(authoritiesForOrigin("SOMETHING_ELSE").contains("ROLE_ADMIN"));
    }
}
