package fr.centralesupelec.thuv.lti;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fr.centralesupelec.thuv.activity_logging.services.ActivityLogger;
import fr.centralesupelec.thuv.lti.repository.ToolDeploymentRepository;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.security.JwtTokenProvider;
import fr.centralesupelec.thuv.security.MyUserDetailsService;
import fr.centralesupelec.thuv.security.dtos.TokenOrigin;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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
 * What the launch handler does with the outcome of the checks: a refused binding must stop the
 * request before a session token is minted or a role is granted, and an Instructor claim from any
 * registered platform must still grant the teacher role, whichever issuer it carries.
 */
class LtiControllerLaunchTest {
    private static final String ISSUER = "https://moodle.example.org";
    private static final String ANOTHER_ISSUER = "https://lms.example.net";
    private static final String STATE = "state-value";
    private static final String ID_TOKEN = "id-token";

    private LtiService ltiService;
    private MyUserDetailsService myUserDetailsService;
    private JwtTokenProvider jwtTokenProvider;
    private ActivityLogger activityLogger;
    private LtiController controller;
    private HttpSession session;

    @BeforeEach
    void setUp() {
        ltiService = mock(LtiService.class);
        myUserDetailsService = mock(MyUserDetailsService.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        activityLogger = mock(ActivityLogger.class);
        session = mock(HttpSession.class);

        Cache<String, Boolean> noncesCache = CacheBuilder.newBuilder().build();
        controller = new LtiController(
                mock(ToolDeploymentRepository.class),
                ltiService,
                noncesCache,
                new RestTemplate(),
                myUserDetailsService,
                jwtTokenProvider,
                "https://mydocker.example.org/api",
                "https://mydocker.example.org",
                mock(LtiJwtService.class),
                activityLogger
        );
    }

    private DecodedJWT launchJwt(boolean instructor) {
        return launchJwt(instructor, ISSUER);
    }

    private DecodedJWT launchJwt(boolean instructor, String issuer) {
        String token = JWT.create()
                .withIssuer(issuer)
                .withClaim("email", "someone@example.org")
                .withClaim("given_name", "Some")
                .withClaim("family_name", "One")
                .withClaim(
                        LtiConfig.Claims.ROLES,
                        instructor
                                ? List.of(LtiConfig.Roles.INSTRUCTOR)
                                : List.of("http://purl.imsglobal.org/vocab/lis/v2/membership#Learner")
                )
                .withClaim(LtiConfig.Claims.MESSAGE_TYPE, "LtiResourceLinkRequest")
                .withClaim("https://purl.imsglobal.org/spec/lti/claim/target_link_uri", "https://mydocker.example.org")
                .sign(Algorithm.HMAC256("test"));
        return JWT.decode(token);
    }

    private DecodedJWT launchJwtWithoutEmail() {
        String token = JWT.create()
                .withIssuer(ISSUER)
                .withClaim("given_name", "Some")
                .withClaim("family_name", "One")
                .withClaim(LtiConfig.Claims.MESSAGE_TYPE, "LtiResourceLinkRequest")
                .withClaim(LtiConfig.Claims.TARGET_LINK_URI, "https://mydocker.example.org")
                .sign(Algorithm.HMAC256("test"));
        return JWT.decode(token);
    }

    private DecodedJWT launchJwtWithMessageType(String messageType) {
        var builder = JWT.create()
                .withIssuer(ISSUER)
                .withClaim("email", "someone@example.org")
                .withClaim("given_name", "Some")
                .withClaim("family_name", "One")
                .withClaim(LtiConfig.Claims.TARGET_LINK_URI, "https://mydocker.example.org");
        if (null != messageType) {
            builder = builder.withClaim(LtiConfig.Claims.MESSAGE_TYPE, messageType);
        }
        return JWT.decode(builder.sign(Algorithm.HMAC256("test")));
    }

    private void assertNothingWasWritten() {
        verify(myUserDetailsService, never()).upsertUserFromUntrustedOrigin(
                any(), anyString(), any(), any(), any()
        );
        verify(myUserDetailsService, never()).ensureTeacher(any());
        verify(jwtTokenProvider, never()).generateToken(any(), any(), any());
        verify(activityLogger, never()).log(any(), any());
    }

    private User ordinaryUser() {
        User user = new User();
        user.setId(7L);
        user.setEmail("someone@example.org");
        user.setUsername("someone@example.org");
        return user;
    }

    @Test
    void aRefusedBindingMintsNoTokenAndGrantsNoRole() {
        when(ltiService.getValidatedJWT(eq(ID_TOKEN), eq(STATE), any())).thenReturn(launchJwt(true));
        when(myUserDetailsService.upsertUserFromUntrustedOrigin(
                any(), anyString(), any(), any(), eq(TokenOrigin.LTI)
        )).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "refused"));

        assertThrows(
                ResponseStatusException.class,
                () -> controller.launch(ID_TOKEN, STATE, session)
        );

        verify(jwtTokenProvider, never()).generateToken(any(), any(), any());
        verify(myUserDetailsService, never()).ensureTeacher(any());
        verify(activityLogger, never()).log(any(), any());
    }

    /**
     * The email claim is the launch's only identity, used as both lookup key and stored address.
     * Without it the upsert resolves nothing and saves a row with a null username and null email,
     * so the refusal has to come before the write rather than after it.
     */
    @Test
    void aLaunchWithNoEmailClaimWritesNothing() {
        when(ltiService.getValidatedJWT(eq(ID_TOKEN), eq(STATE), any())).thenReturn(launchJwtWithoutEmail());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.launch(ID_TOKEN, STATE, session)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertNothingWasWritten();
    }

    @Test
    void anInstructorClaimGrantsTheTeacherRole() {
        User user = ordinaryUser();
        when(ltiService.getValidatedJWT(eq(ID_TOKEN), eq(STATE), any())).thenReturn(launchJwt(true));
        when(myUserDetailsService.upsertUserFromUntrustedOrigin(
                any(), anyString(), any(), any(), eq(TokenOrigin.LTI)
        )).thenReturn(user);

        controller.launch(ID_TOKEN, STATE, session);

        verify(myUserDetailsService).ensureTeacher(user);
    }

    /**
     * The grant must not depend on which issuer the launch carries. An issuer allow-list here would
     * pass every other test in this class while stopping teacher promotion on the platforms already
     * registered in production, so assert issuer independence explicitly.
     */
    @Test
    void anInstructorClaimFromAnyOtherIssuerAlsoGrantsTheTeacherRole() {
        User user = ordinaryUser();
        when(ltiService.getValidatedJWT(eq(ID_TOKEN), eq(STATE), any()))
                .thenReturn(launchJwt(true, ANOTHER_ISSUER));
        when(myUserDetailsService.upsertUserFromUntrustedOrigin(
                any(), anyString(), any(), any(), eq(TokenOrigin.LTI)
        )).thenReturn(user);

        controller.launch(ID_TOKEN, STATE, session);

        verify(myUserDetailsService).ensureTeacher(user);
    }

    @Test
    void aLearnerLaunchStillGrantsNoTeacherRole() {
        when(ltiService.getValidatedJWT(eq(ID_TOKEN), eq(STATE), any())).thenReturn(launchJwt(false));
        when(myUserDetailsService.upsertUserFromUntrustedOrigin(
                any(), anyString(), any(), any(), eq(TokenOrigin.LTI)
        )).thenReturn(ordinaryUser());

        controller.launch(ID_TOKEN, STATE, session);

        verify(myUserDetailsService, never()).ensureTeacher(any());
    }

    @Test
    void anUnsupportedMessageTypeIsRejectedBeforeAnythingIsWritten() {
        when(ltiService.getValidatedJWT(eq(ID_TOKEN), eq(STATE), any()))
                .thenReturn(launchJwtWithMessageType("LtiSomethingElseRequest"));

        assertThrows(
                ResponseStatusException.class,
                () -> controller.launch(ID_TOKEN, STATE, session)
        );
        assertNothingWasWritten();
    }

    @Test
    void aMissingMessageTypeIsRejectedRatherThanThrowing() {
        when(ltiService.getValidatedJWT(eq(ID_TOKEN), eq(STATE), any()))
                .thenReturn(launchJwtWithMessageType(null));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.launch(ID_TOKEN, STATE, session)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertNothingWasWritten();
    }

    @Test
    void aResourceLinkWithNoTargetIsRejectedBeforeAnythingIsWritten() {
        String token = JWT.create()
                .withIssuer(ISSUER)
                .withClaim("email", "someone@example.org")
                .withClaim(LtiConfig.Claims.MESSAGE_TYPE, LtiConfig.MessageTypes.RESOURCE_LINK)
                .sign(Algorithm.HMAC256("test"));
        when(ltiService.getValidatedJWT(eq(ID_TOKEN), eq(STATE), any())).thenReturn(JWT.decode(token));

        assertThrows(
                ResponseStatusException.class,
                () -> controller.launch(ID_TOKEN, STATE, session)
        );
        assertNothingWasWritten();
    }

    @Test
    void aMalformedTargetIsRejectedBeforeAnythingIsWritten() {
        String token = JWT.create()
                .withIssuer(ISSUER)
                .withClaim("email", "someone@example.org")
                .withClaim(LtiConfig.Claims.MESSAGE_TYPE, LtiConfig.MessageTypes.RESOURCE_LINK)
                .withClaim(LtiConfig.Claims.TARGET_LINK_URI, "not-a-url")
                .sign(Algorithm.HMAC256("test"));
        when(ltiService.getValidatedJWT(eq(ID_TOKEN), eq(STATE), any())).thenReturn(JWT.decode(token));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.launch(ID_TOKEN, STATE, session)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertNothingWasWritten();
    }
}
