package fr.centralesupelec.thuv.lti;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fr.centralesupelec.thuv.activity_logging.services.ActivityLogger;
import fr.centralesupelec.thuv.lti.dtos.OpenIdConfigurationDto;
import fr.centralesupelec.thuv.lti.model.ToolDeployment;
import fr.centralesupelec.thuv.lti.repository.ToolDeploymentRepository;
import fr.centralesupelec.thuv.security.JwtTokenProvider;
import fr.centralesupelec.thuv.security.MyUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * What registration does when a platform we already know registers again. An issuer carries one row
 * per espace de cours, so a second registration on a known Moodle is the normal case; only the same
 * pair twice is a duplicate, and a duplicate never rewrites what is already stored.
 */
class LtiControllerRegisterTest {
    private static final String ISSUER = "https://moodle.example.org";
    private static final String CONFIGURATION_URL = "https://moodle.example.org/.well-known/openid-configuration";
    private static final String REGISTRATION_TOKEN = "registration-token";

    private ToolDeploymentRepository toolDeploymentRepository;
    private LtiService ltiService;
    private RestTemplate ltiRestTemplate;
    private LtiController controller;

    @BeforeEach
    void setUp() {
        toolDeploymentRepository = mock(ToolDeploymentRepository.class);
        ltiService = mock(LtiService.class);
        ltiRestTemplate = mock(RestTemplate.class);

        Cache<String, Boolean> noncesCache = CacheBuilder.newBuilder().build();
        controller = new LtiController(
                toolDeploymentRepository,
                ltiService,
                noncesCache,
                ltiRestTemplate,
                mock(MyUserDetailsService.class),
                mock(JwtTokenProvider.class),
                "https://mydocker.example.org/api",
                "https://mydocker.example.org",
                mock(LtiJwtService.class),
                mock(ActivityLogger.class)
        );
    }

    private void platformAnnounces(String clientId) throws Exception {
        OpenIdConfigurationDto configuration = new OpenIdConfigurationDto();
        configuration.setIssuer(ISSUER);
        configuration.setJwksUri("https://moodle.example.org/jwks");
        configuration.setAuthorizationEndpoint("https://moodle.example.org/auth");
        configuration.setRegistrationEndpoint("https://moodle.example.org/register");

        when(ltiRestTemplate.getForObject(eq(CONFIGURATION_URL), eq(OpenIdConfigurationDto.class)))
                .thenReturn(configuration);
        when(ltiService.registerTool(anyString(), any())).thenReturn(clientId);
    }

    /** The case that fails before this change: a second espace de cours on a known Moodle. */
    @Test
    void aSecondIntegrationOnAKnownPlatformIsRegistered() throws Exception {
        platformAnnounces("client-2");
        when(toolDeploymentRepository.findByIssAndClientId(ISSUER, "client-2")).thenReturn(Optional.empty());

        controller.register(CONFIGURATION_URL, REGISTRATION_TOKEN);

        ArgumentCaptor<ToolDeployment> saved = ArgumentCaptor.forClass(ToolDeployment.class);
        verify(toolDeploymentRepository).saveAndFlush(saved.capture());
        assertEquals(ISSUER, saved.getValue().getIss());
        assertEquals("client-2", saved.getValue().getClientId());
    }

    /**
     * Two registrations of the same pair can both pass the lookup before either writes. The unique
     * constraint is what keeps the data correct; the caller must still see the same answer the
     * lookup gives rather than a 500.
     */
    @Test
    void aConcurrentDuplicateRegistrationIsAConflictRatherThanAServerError() throws Exception {
        platformAnnounces("client-3");
        when(toolDeploymentRepository.findByIssAndClientId(ISSUER, "client-3")).thenReturn(Optional.empty());
        when(toolDeploymentRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("uk_tool_deployments_iss_client_id"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.register(CONFIGURATION_URL, REGISTRATION_TOKEN)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    /**
     * The attempted repoint: registering the same pair again must leave the stored row alone. The
     * configuration URL is caller-supplied and the client id comes back from the endpoint it names,
     * so accepting this as an update would let one caller replace another deployment's jwks_uri.
     */
    @Test
    void registeringAnExistingPairIsRefusedAndWritesNothing() throws Exception {
        platformAnnounces("client-1");
        ToolDeployment stored = ToolDeployment
                .builder()
                .iss(ISSUER)
                .clientId("client-1")
                .jwksUri("https://moodle.example.org/original-jwks")
                .authorizationEndpoint("https://moodle.example.org/original-auth")
                .build();
        when(toolDeploymentRepository.findByIssAndClientId(ISSUER, "client-1")).thenReturn(Optional.of(stored));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.register(CONFIGURATION_URL, REGISTRATION_TOKEN)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(toolDeploymentRepository, never()).saveAndFlush(any());
        assertEquals("https://moodle.example.org/original-jwks", stored.getJwksUri());
        assertEquals("https://moodle.example.org/original-auth", stored.getAuthorizationEndpoint());
    }
}
