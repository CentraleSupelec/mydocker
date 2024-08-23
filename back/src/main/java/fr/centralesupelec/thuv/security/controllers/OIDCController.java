package fr.centralesupelec.thuv.security.controllers;

import fr.centralesupelec.thuv.security.OIDCService;
import fr.centralesupelec.thuv.security.dtos.OIDCToken;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/oidc")
public class OIDCController {
    private static final Logger logger = LoggerFactory.getLogger(OIDCController.class);
    private final OIDCService oidcService;

    @PostMapping(value = "/")
    public ResponseEntity<?> login(@RequestBody OIDCToken token) {
        logger.debug("OIDC Token : " + token.getAccessToken());
        String jwtToken = oidcService.getLocalTokenFromOIDCToken(token.getAccessToken());
        try {
            return ResponseEntity.ok(jwtToken);
        } catch (Exception ex) {
            Sentry.captureException(ex);
            logger.error("An error occured during OIDC token verification " + ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occured during OIDC token verification ... :(");
        }
    }
}
