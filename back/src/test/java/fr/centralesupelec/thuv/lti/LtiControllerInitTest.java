package fr.centralesupelec.thuv.lti;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fr.centralesupelec.thuv.activity_logging.services.ActivityLogger;
import fr.centralesupelec.thuv.lti.model.ToolDeployment;
import fr.centralesupelec.thuv.lti.repository.ToolDeploymentRepository;
import fr.centralesupelec.thuv.security.JwtTokenProvider;
import fr.centralesupelec.thuv.security.MyUserDetailsService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * How a login initiation resolves the deployment it is for. One integration is registered per espace
 * de cours, so an issuer carries many rows, and a client id is only unique within its issuer: the
 * pair is the identity, and neither half alone will do.
 */
class LtiControllerInitTest {
    private static final String ISSUER = "https://moodle.example.org";
    private static final String OTHER_ISSUER = "https://lms.example.net";
    private static final String SHARED_CLIENT_ID = "client-42";
    private static final String TARGET_LINK_URI = "https://mydocker.example.org";
    private static final String LOGIN_HINT = "login-hint";

    private ToolDeploymentRepository toolDeploymentRepository;
    private LtiController controller;
    private HttpSession session;

    @BeforeEach
    void setUp() {
        toolDeploymentRepository = mock(ToolDeploymentRepository.class);
        session = mock(HttpSession.class);

        Cache<String, Boolean> noncesCache = CacheBuilder.newBuilder().build();
        controller = new LtiController(
                toolDeploymentRepository,
                mock(LtiService.class),
                noncesCache,
                new RestTemplate(),
                mock(MyUserDetailsService.class),
                mock(JwtTokenProvider.class),
                "https://mydocker.example.org/api",
                "https://mydocker.example.org",
                mock(LtiJwtService.class),
                mock(ActivityLogger.class),
                "https://mydocker.example.org/lti/refused"
        );
    }

    private ToolDeployment deployment(String iss, String clientId, String authorizationEndpoint) {
        return ToolDeployment
                .builder()
                .iss(iss)
                .clientId(clientId)
                .jwksUri(String.format("%s/jwks", iss))
                .authorizationEndpoint(authorizationEndpoint)
                .build();
    }

    private ResponseEntity<String> init(Optional<String> clientId) {
        return controller.init(
                ISSUER,
                TARGET_LINK_URI,
                LOGIN_HINT,
                Optional.of("message-hint"),
                clientId,
                Optional.empty(),
                session
        );
    }

    /**
     * The case this test exists for: two platforms happen to issue the same client id, and the
     * request must reach the row belonging to the issuer it names rather than whichever row the
     * client id matches first.
     */
    @Test
    void aSharedClientIdResolvesTheRowOfTheIssuerNamedInTheRequest() {
        when(toolDeploymentRepository.findByIssAndClientId(ISSUER, SHARED_CLIENT_ID)).thenReturn(
                Optional.of(deployment(ISSUER, SHARED_CLIENT_ID, "https://moodle.example.org/auth"))
        );
        when(toolDeploymentRepository.findByIssAndClientId(OTHER_ISSUER, SHARED_CLIENT_ID)).thenReturn(
                Optional.of(deployment(OTHER_ISSUER, SHARED_CLIENT_ID, "https://lms.example.net/auth"))
        );

        ResponseEntity<String> response = init(Optional.of(SHARED_CLIENT_ID));

        String location = response.getHeaders().getLocation().toString();
        assertTrue(location.startsWith("https://moodle.example.org/auth"), location);
    }

    @Test
    void anIssuerWithASingleIntegrationStillResolvesWithoutAClientId() {
        when(toolDeploymentRepository.findAllByIss(ISSUER)).thenReturn(
                List.of(deployment(ISSUER, "client-1", "https://moodle.example.org/auth"))
        );

        ResponseEntity<String> response = init(Optional.empty());

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
    }

    @Test
    void anIssuerWithSeveralIntegrationsIsAmbiguousWithoutAClientId() {
        when(toolDeploymentRepository.findAllByIss(ISSUER)).thenReturn(
                List.of(
                        deployment(ISSUER, "client-1", "https://moodle.example.org/auth"),
                        deployment(ISSUER, "client-2", "https://moodle.example.org/auth")
                )
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> init(Optional.empty())
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void anUnknownPlatformIsStillNotFound() {
        when(toolDeploymentRepository.findAllByIss(ISSUER)).thenReturn(List.of());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> init(Optional.empty())
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void anUnknownPairIsNotFound() {
        when(toolDeploymentRepository.findByIssAndClientId(ISSUER, SHARED_CLIENT_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> init(Optional.of(SHARED_CLIENT_ID))
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
