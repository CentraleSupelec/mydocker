package fr.centralesupelec.thuv.lti;

import com.auth0.jwk.JwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import fr.centralesupelec.thuv.lti.model.ToolDeployment;
import fr.centralesupelec.thuv.lti.repository.ToolDeploymentRepository;
import fr.centralesupelec.thuv.model.Role;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.repository.RoleRepository;
import fr.centralesupelec.thuv.repository.UserRepository;
import fr.centralesupelec.thuv.security.OIDCConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A real LTI connection, end to end, against a platform this test runs. Nothing in the LTI path is
 * stubbed: the platform is registered through the live registration route, the login initiation
 * produces a real state and nonce, and the launch token is signed with a key served from the
 * platform's own JWKS endpoint and verified by fetching it over HTTP.
 *
 * <p>This is the closest thing to clicking an activity in an LMS that can run without one.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(
        locations = "classpath:application-test.properties",
        properties = {
                "lti.private-key-path=classpath:lti-test-private.pem",
                "lti.public-key-path=classpath:lti-test-public.pem"
        }
)
class LtiEndToEndLaunchTest {
    private static final String LAUNCH_EMAIL = "student@example.org";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ToolDeploymentRepository toolDeploymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    // Replaced only because the real bean fetches OIDC discovery over the network at startup.
    @MockitoBean(name = OIDCConfig.JWK_BEAN_NAME)
    private JwkProvider jwkProvider;

    private HttpServer platform;
    private String platformUrl;
    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
    private final String keyId = "platform-key";

    @BeforeEach
    void startPlatform() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        publicKey = (RSAPublicKey) keyPair.getPublic();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();

        platform = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        platformUrl = "http://127.0.0.1:" + platform.getAddress().getPort();

        platform.createContext("/.well-known/openid-configuration", exchange -> respond(exchange, String.format(
                "{\"issuer\":\"%s\",\"jwks_uri\":\"%s/jwks\",\"authorization_endpoint\":\"%s/auth\","
                        + "\"registration_endpoint\":\"%s/register\",\"token_endpoint\":\"%s/token\"}",
                platformUrl, platformUrl, platformUrl, platformUrl, platformUrl
        )));
        platform.createContext("/register", exchange -> respond(
                exchange, "{\"client_id\":\"client-" + UUID.randomUUID() + "\"}"
        ));
        platform.createContext("/jwks", exchange -> respond(exchange, String.format(
                "{\"keys\":[{\"kty\":\"RSA\",\"kid\":\"%s\",\"alg\":\"RS256\",\"use\":\"sig\","
                        + "\"n\":\"%s\",\"e\":\"%s\"}]}",
                keyId, base64Url(publicKey.getModulus()), base64Url(publicKey.getPublicExponent())
        )));
        platform.start();
    }

    @AfterEach
    void stopPlatform() {
        platform.stop(0);
    }

    private static void respond(HttpExchange exchange, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String base64Url(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] trimmed = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
            bytes = trimmed;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Registers the platform through the live route, exactly as an administrator would. */
    private ToolDeployment registerPlatform() throws Exception {
        mockMvc.perform(get(LtiConfig.REGISTER_PATH)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("admin").roles("ADMIN"))
                        .param("openid_configuration", platformUrl + "/.well-known/openid-configuration")
                        .param("registration_token", "registration-token"))
                .andExpect(status().isOk());
        return toolDeploymentRepository.findByIss(platformUrl).orElseThrow();
    }

    /** Runs the OIDC login initiation and returns the state and nonce the platform must echo back. */
    private Map<String, String> initiateLogin(ToolDeployment deployment, MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(post(LtiConfig.INIT_PATH)
                        .session(session)
                        .param("iss", platformUrl)
                        .param("target_link_uri", "https://mydocker.example.org/course")
                        .param("login_hint", "login-hint")
                        .param("client_id", deployment.getClientId()))
                .andExpect(status().isFound())
                .andReturn();

        String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
        assertNotNull(location);
        var query = UriComponentsBuilder.fromUriString(location).build().getQueryParams();
        return Map.of(
                "state", query.getFirst("state"),
                "nonce", query.getFirst("nonce")
        );
    }

    private String launchToken(ToolDeployment deployment, String nonce, String email, boolean instructor) {
        Date now = new Date();
        return JWT.create()
                .withKeyId(keyId)
                .withIssuer(platformUrl)
                .withAudience(deployment.getClientId())
                .withIssuedAt(now)
                .withExpiresAt(new Date(now.getTime() + 60_000))
                .withClaim("nonce", nonce)
                .withClaim("email", email)
                .withClaim("given_name", "Launched")
                .withClaim("family_name", "User")
                .withClaim(LtiConfig.Claims.ROLES, instructor
                        ? List.of(LtiConfig.Roles.INSTRUCTOR)
                        : List.of("http://purl.imsglobal.org/vocab/lis/v2/membership#Learner"))
                .withClaim(LtiConfig.Claims.MESSAGE_TYPE, LtiConfig.MessageTypes.RESOURCE_LINK)
                .withClaim(LtiConfig.Claims.TARGET_LINK_URI, "https://mydocker.example.org/course")
                .sign(Algorithm.RSA256(publicKey, privateKey));
    }

    private String sessionTokenFrom(MvcResult result) {
        String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
        assertNotNull(location);
        return UriComponentsBuilder.fromUriString(location).build().getQueryParams().getFirst("idtoken");
    }

    @Test
    void aStudentCanConnectThroughARegisteredPlatform() throws Exception {
        ToolDeployment deployment = registerPlatform();
        MockHttpSession session = new MockHttpSession();
        Map<String, String> initiation = initiateLogin(deployment, session);

        MvcResult launch = mockMvc.perform(post(LtiConfig.LAUNCH_PATH)
                        .session(session)
                        .param("id_token", launchToken(deployment, initiation.get("nonce"), LAUNCH_EMAIL, false))
                        .param("state", initiation.get("state")))
                .andExpect(status().isFound())
                .andReturn();

        String sessionToken = sessionTokenFrom(launch);
        assertNotNull(sessionToken, "the launch must hand the browser a MyDocker session token");
        assertTrue(userRepository.findByEmail(LAUNCH_EMAIL).size() == 1, "the account must exist after the launch");

        // The session works as a normal user,
        mockMvc.perform(get("/courses/joined").header("Authorization", "Bearer " + sessionToken))
                .andExpect(status().isOk());

        // and it is not an administrator session.
        mockMvc.perform(get("/admin/contents/").header("Authorization", "Bearer " + sessionToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void replayingTheSameLaunchIsRefused() throws Exception {
        ToolDeployment deployment = registerPlatform();
        MockHttpSession session = new MockHttpSession();
        Map<String, String> initiation = initiateLogin(deployment, session);
        String token = launchToken(deployment, initiation.get("nonce"), LAUNCH_EMAIL, false);

        mockMvc.perform(post(LtiConfig.LAUNCH_PATH).session(session)
                        .param("id_token", token).param("state", initiation.get("state")))
                .andExpect(status().isFound());

        mockMvc.perform(post(LtiConfig.LAUNCH_PATH).session(session)
                        .param("id_token", token).param("state", initiation.get("state")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aLaunchClaimingAnAdministratorEmailIsRefused() throws Exception {
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
        String adminEmail = "admin-" + UUID.randomUUID() + "@example.org";
        User administrator = new User();
        administrator.setEmail(adminEmail);
        administrator.setUsername(adminEmail);
        administrator.setName("Real");
        administrator.setLastname("Administrator");
        administrator.setEnabled(true);
        administrator.setRoles(new ArrayList<>(List.of(adminRole)));
        userRepository.saveAndFlush(administrator);

        ToolDeployment deployment = registerPlatform();
        MockHttpSession session = new MockHttpSession();
        Map<String, String> initiation = initiateLogin(deployment, session);

        // Withholding roles would not be enough: permission services authorise by ownership and by
        // explicit permission rows keyed on the user id, so binding to the account at all would
        // hand the platform whatever that administrator owns. The launch is refused outright.
        MvcResult launch = mockMvc.perform(post(LtiConfig.LAUNCH_PATH)
                        .session(session)
                        .param("id_token", launchToken(deployment, initiation.get("nonce"), adminEmail, true))
                        .param("state", initiation.get("state")))
                .andExpect(status().isForbidden())
                .andReturn();

        assertNull(launch.getResponse().getHeader(HttpHeaders.LOCATION), "no session may be handed back");

        User after = userRepository.findByEmail(adminEmail).get(0);
        assertEquals("Real", after.getName());
        assertEquals("Administrator", after.getLastname());
        assertTrue(
                after.getRoles().stream().noneMatch(role -> "ROLE_TEACHER".equals(role.getName())),
                "a refused launch must not grant the teacher role"
        );
    }
}
