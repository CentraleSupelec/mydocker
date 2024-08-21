package fr.centralesupelec.thuv.security.controllers;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.centralesupelec.thuv.activity_logging.model.LogAction;
import fr.centralesupelec.thuv.activity_logging.services.ActivityLogger;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.security.JwtTokenProvider;
import fr.centralesupelec.thuv.security.MyUserDetailsService;
import fr.centralesupelec.thuv.security.dtos.OIDCToken;
import fr.centralesupelec.thuv.security.dtos.TokenOrigin;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/oidc")
public class OIDCController {
    private static final Logger logger = LoggerFactory.getLogger(OIDCController.class);
    private final MyUserDetailsService myUserDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final ActivityLogger activityLogger;
    private final RestTemplate restTemplate;

    @Value("#{'${oidc.allowed-audiences}'.split(',')}")
    private List<String> allowedAudiences;
    @Value("#{'${oidc.allowed-issuers}'.split(',')}")
    private List<String> allowedIssuers;

    private static final int JWKSCacheSize = 10;
    private static final int JWKSCacheExpiresIn = 24;
    private static final long AcceptedLeeway = 10L;

    @PostMapping(value = "/")
    public ResponseEntity<?> login(@RequestBody OIDCToken token) {
        logger.debug("OIDC Token : " + token.getAccessToken());
        DecodedJWT jwt = JWT.decode(token.getAccessToken());
        try {
            // TODO : Cache openid configuration
            String responseBody = restTemplate.getForObject(
                    String.format("%s/%s", jwt.getIssuer(), ".well-known/openid-configuration"),
                    String.class
            );
            ObjectMapper objectMapper = new ObjectMapper();
            String jwksUriValue = objectMapper.readTree(responseBody).get("jwks_uri").asText();

            JwkProvider provider = new JwkProviderBuilder(new URL(jwksUriValue))
                    .cached(JWKSCacheSize, JWKSCacheExpiresIn, TimeUnit.HOURS)
                    .build();
            logger.debug(String.format("Retrieving key of id '%s'", jwt.getKeyId()));
            Jwk jwk = provider.get(jwt.getKeyId());
            Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
            JWTVerifier jwtVerifier = JWT.require(algorithm)
                    .acceptLeeway(AcceptedLeeway)
                    .withIssuer(this.allowedIssuers.toArray(new String[0]))
                    .withClaim("azp", (claim, decodedJWT) -> this.allowedAudiences.contains(claim.asString()))
                    .build();
            jwtVerifier.verify(token.getAccessToken());

            String email = jwt.getClaim("preferred_username").asString();
            String name = jwt.getClaim("given_name").asString();
            String lastName = jwt.getClaim("family_name").asString();

            // FindorCreate user

            User user = myUserDetailsService.upsertUser(email, name, lastName);
            List<String> roles = (List<String>) jwt.getClaim("realm_access").asMap().get("roles");
            if (roles.contains("teacher")) {
                myUserDetailsService.ensureTeacher(user);
            }

            String jwtToken = jwtTokenProvider.generateToken(user.getEmail(), TokenOrigin.OIDC);
            logger.debug("JWT token: " + jwtToken);
            activityLogger.log(LogAction.USER_LOGIN_OIDC, user);
            return ResponseEntity.ok(jwtToken);
        } catch (Exception ex) {
            Sentry.captureException(ex);
            logger.error("An error occured during OIDC token verification " + ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occured during OIDC token verification ... :(");
        }
    }
}
