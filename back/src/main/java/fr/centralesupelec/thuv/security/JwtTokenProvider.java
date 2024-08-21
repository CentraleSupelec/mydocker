package fr.centralesupelec.thuv.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import fr.centralesupelec.thuv.security.dtos.TokenOrigin;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtTokenProvider {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final int jwtExpirationInMs;

    private final Algorithm algorithmHS256;
    private final JWTVerifier verifier;

    public JwtTokenProvider(
            @Value("${app.jwtSecret}") String secret,
            @Value("${app.jwtExpirationInMs}") int expirationInMs
    ) {
        jwtExpirationInMs = expirationInMs;
        algorithmHS256 = Algorithm.HMAC256(secret);
        verifier = JWT.require(algorithmHS256).build();
    }

    public String generateToken(String username, TokenOrigin tokenOrigin) {

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        logger.info("Signing JWT for " + username);

        return JWT.create()
                .withIssuer("thuv")
                .withExpiresAt(expiryDate)
                .withIssuedAt(now)
                .withSubject(username)
                .withClaim("origin", tokenOrigin.toString())
                .sign(algorithmHS256);
    }

    public String getUsernameFromJWT(String token) {
        DecodedJWT jwt = JWT.decode(token);
        return jwt.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException ex) {
            if (!(ex instanceof TokenExpiredException)) {
                Sentry.captureException(ex);
            }
            logger.warn("Invalid JWT signature: " + ex);
            return false;
        }
    }
}
