package fr.centralesupelec.thuv.lti;

import com.auth0.jwk.Jwk;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fr.centralesupelec.thuv.lti.model.ToolDeployment;
import fr.centralesupelec.thuv.lti.repository.ToolDeploymentRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The launch checks that decide whether a token minted elsewhere is allowed to open a session here.
 * The key lookup is stubbed through the package-private seam, so nothing reaches the network.
 */
class LtiServiceLaunchValidationTest {
    private static final String ISSUER = "https://moodle.example.org";
    private static final String CLIENT_ID = "registered-client-id";
    private static final String KEY_ID = "test-key";
    private static final String STATE = "state-value";

    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
    private Cache<String, Boolean> noncesCache;
    private LtiService service;
    private HttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        publicKey = (RSAPublicKey) keyPair.getPublic();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();

        ToolDeployment deployment = ToolDeployment
                .builder()
                .iss(ISSUER)
                .clientId(CLIENT_ID)
                .jwksUri("https://moodle.example.org/jwks")
                .authorizationEndpoint("https://moodle.example.org/auth")
                .build();

        ToolDeploymentRepository repository = mock(ToolDeploymentRepository.class);
        when(repository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(deployment));

        noncesCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build();

        service = new LtiService(
                noncesCache,
                new RestTemplate(),
                repository,
                "https://mydocker.example.org",
                "mydocker.example.org",
                "https://mydocker.example.org/api"
        ) {
            @Override
            Jwk getJwk(String jwksUri, String keyId) {
                Map<String, Object> values = new HashMap<>();
                values.put("kty", "RSA");
                values.put("kid", KEY_ID);
                values.put("alg", "RS256");
                values.put("use", "sig");
                values.put("n", base64Url(publicKey.getModulus()));
                values.put("e", base64Url(publicKey.getPublicExponent()));
                return Jwk.fromValues(values);
            }
        };

        session = mock(HttpSession.class);
        when(session.getAttribute(LtiConfig.STATE_ATTRIBUTE)).thenReturn(STATE);
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

    private String launchToken(String issuer, String nonce) {
        Date now = new Date();
        return JWT.create()
                .withKeyId(KEY_ID)
                .withIssuer(issuer)
                .withAudience(CLIENT_ID)
                .withIssuedAt(now)
                .withExpiresAt(new Date(now.getTime() + 60_000))
                .withClaim("nonce", nonce)
                .sign(Algorithm.RSA256(publicKey, privateKey));
    }

    private String issuedNonce() {
        String nonce = UUID.randomUUID().toString();
        noncesCache.put(nonce, false);
        return nonce;
    }

    @Test
    void aLaunchFromTheRegisteredPlatformIsAccepted() {
        String token = launchToken(ISSUER, issuedNonce());
        assertDoesNotThrow(() -> service.getValidatedJWT(token, STATE, session));
    }

    @Test
    void aTokenWhoseIssuerIsNotTheRegisteredOneIsRejected() {
        String token = launchToken("https://attacker.example.com", issuedNonce());
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getValidatedJWT(token, STATE, session)
        );
        assertEquals("Issuer invalid", exception.getReason());
    }

    @Test
    void aTokenWithNoIssuerAtAllIsRejected() {
        Date now = new Date();
        String token = JWT.create()
                .withKeyId(KEY_ID)
                .withAudience(CLIENT_ID)
                .withIssuedAt(now)
                .withExpiresAt(new Date(now.getTime() + 60_000))
                .withClaim("nonce", issuedNonce())
                .sign(Algorithm.RSA256(publicKey, privateKey));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getValidatedJWT(token, STATE, session)
        );
        assertEquals("Issuer invalid", exception.getReason());
    }

    @Test
    void aNonceWeNeverIssuedIsRejected() {
        String token = launchToken(ISSUER, UUID.randomUUID().toString());
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getValidatedJWT(token, STATE, session)
        );
        assertEquals("Nonce invalid", exception.getReason());
    }

    @Test
    void aReplayedLaunchIsRejected() {
        String token = launchToken(ISSUER, issuedNonce());
        assertDoesNotThrow(() -> service.getValidatedJWT(token, STATE, session));
        assertThrows(
                ResponseStatusException.class,
                () -> service.getValidatedJWT(token, STATE, session)
        );
    }

    @Test
    void twoConcurrentLaunchesOnOneNonceLeaveExactlyOneWinner() throws Exception {
        String token = launchToken(ISSUER, issuedNonce());
        AtomicInteger accepted = new AtomicInteger();
        List<Exception> rejections = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try {
                    start.await();
                    service.getValidatedJWT(token, STATE, session);
                    accepted.incrementAndGet();
                } catch (Exception ex) {
                    rejections.add(ex);
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        boolean finished = done.await(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertTrue(finished, "both launches should have completed");
        assertEquals(1, accepted.get());
        assertEquals(1, rejections.size());
        ResponseStatusException rejection = assertInstanceOf(ResponseStatusException.class, rejections.get(0));
        assertEquals("Nonce invalid", rejection.getReason());
    }
}
