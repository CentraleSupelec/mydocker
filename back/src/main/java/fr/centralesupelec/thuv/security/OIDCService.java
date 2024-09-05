package fr.centralesupelec.thuv.security;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.cache.Cache;
import fr.centralesupelec.thuv.activity_logging.model.LogAction;
import fr.centralesupelec.thuv.activity_logging.services.ActivityLogger;
import fr.centralesupelec.thuv.exception.OIDCAuthenticationException;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.security.dtos.TokenOrigin;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.security.interfaces.RSAPublicKey;
import java.util.List;

@RequiredArgsConstructor
@Component
public class OIDCService {
    private static final Logger logger = LoggerFactory.getLogger(OIDCService.class);
    private final MyUserDetailsService myUserDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final ActivityLogger activityLogger;
    private final ApplicationContext applicationContext;
    private final Cache<String, String> wellKnownCache;

    @Value("${oidc.audience}")
    private String audience;
    @Value("${oidc.issuer}")
    private String issuer;

    private static final long ACCEPTED_LEEWAY_SECONDS = 10L;

    public String getLocalTokenFromOIDCToken(String oidcToken) {
        try {
            DecodedJWT jwt = JWT.decode(oidcToken);
            this.verifyToken(jwt, 0);

            String username = jwt.getClaim("preferred_username").asString();
            String email = jwt.getClaim("email").asString();
            String name = jwt.getClaim("given_name").asString();
            String lastName = jwt.getClaim("family_name").asString();

            // FindorCreate user

            User user = myUserDetailsService.upsertUser(new String[]{email, username}, name, lastName);
            List<String> roles = (List<String>) jwt.getClaim("realm_access").asMap().get("roles");
            if (roles.contains("teacher")) {
                myUserDetailsService.ensureTeacher(user);
            }

            String jwtToken = jwtTokenProvider.generateToken(user.getEmail(), TokenOrigin.OIDC);
            logger.debug("JWT token: " + jwtToken);
            activityLogger.log(LogAction.USER_LOGIN_OIDC, user);
            return jwtToken;
        } catch (Exception e) {
            OIDCAuthenticationException exception = new OIDCAuthenticationException(
                    String.format("Unable to validate token : %s", e.getMessage())
            );
            exception.initCause(e);
            throw exception;
        }
    }

    private void verifyToken(DecodedJWT jwt, int retries) throws Exception {
        try {
            // Get fresh bean in case it has been destroyed by a prior exception catching
            JwkProvider jwkProvider = applicationContext.getBean(JwkProvider.class);
            Jwk jwk = jwkProvider.get(jwt.getKeyId());
            Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
            JWTVerifier jwtVerifier = JWT.require(algorithm)
                    .acceptLeeway(ACCEPTED_LEEWAY_SECONDS)
                    .withIssuer(this.issuer)
                    .withClaim("azp", this.audience)
                    .build();
            jwtVerifier.verify(jwt);
        } catch (Exception e) {
            if (retries < 1) {
                logger.info(
                        String.format("OIDC verification error: %s, resetting jwks before retrying", e.getMessage())
                );
                DefaultSingletonBeanRegistry registry = (DefaultSingletonBeanRegistry) applicationContext
                        .getAutowireCapableBeanFactory();
                registry.destroySingleton(OIDCConfig.JWK_BEAN_NAME);
                this.wellKnownCache.invalidateAll();
                this.verifyToken(jwt, retries + 1);
            } else {
                throw e;
            }
        }
    }
}
