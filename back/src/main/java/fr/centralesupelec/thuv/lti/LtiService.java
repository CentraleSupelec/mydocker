package fr.centralesupelec.thuv.lti;

import static fr.centralesupelec.thuv.lti.LtiConfig.*;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import fr.centralesupelec.thuv.lti.dtos.OpenIdConfigurationDto;
import fr.centralesupelec.thuv.lti.dtos.RegistrationPayloadDto;
import fr.centralesupelec.thuv.lti.model.ToolDeployment;
import fr.centralesupelec.thuv.lti.repository.ToolDeploymentRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpSession;

import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class LtiService {
    private static final Logger logger = LoggerFactory.getLogger(LtiService.class);
    private final Cache<String, Boolean> noncesCache;
    private final RestTemplate ltiRestTemplate;
    private final ToolDeploymentRepository toolDeploymentRepository;
    private final String baseUrl;
    private final String baseDomain;
    private final String basePath;

    public String registerTool(
            String registrationToken,
            OpenIdConfigurationDto json
    ) throws JsonProcessingException {
        RegistrationPayloadDto registrationPayload = RegistrationPayloadDto
                .builder()
                .clientName("MyDocker")
                .initiateLoginUri(String.format("%s%s", basePath, INIT_PATH))
                .redirectUris(
                        List.of(
                                String.format("%s%s", basePath, LAUNCH_PATH)
                        )
                )
                .jwksUri(String.format("%s%s", basePath, JWKS_PATH))
                .scope("openid")
                .httpsPurlImsglobalOrgSpecLtiToolConfiguration(
                        RegistrationPayloadDto.HttpsPurlImsglobalOrgSpecLtiToolConfigurationDto
                                .builder()
                                .domain(baseDomain)
                                .targetLinkUri(baseUrl)
                                .customParameters(Map.ofEntries())
                                .claims(
                                        List.of(
                                                "email",
                                                "iss",
                                                "sub",
                                                "given_name",
                                                "roles",
                                                "family_name"
                                        )
                                )
                                .messages(List.of())
                                .description("MyDocker")
                                .build()
                )
                .responseTypes(List.of("id_token"))
                .grantTypes(
                        List.of(
                                "implicit"
                        )
                )
                .tokenEndpointAuthMethod("private_key_jwt")
                .idTokenSignedResponseAlg("RS256")
                .logoUri("https://mydocker.centralesupelec.fr/assets/1200px-Logo_CentraleSup%C3%A9lec.svg.png")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(registrationToken);
        HttpEntity<RegistrationPayloadDto> payload = new HttpEntity<>(registrationPayload, headers);

        String registrationResponse = ltiRestTemplate.postForObject(
                json.getRegistrationEndpoint(), payload, String.class
        );
        logger.debug("Registered with response {}", registrationResponse);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(registrationResponse);
        return jsonNode.get("client_id").asText();
    }

    public DecodedJWT getValidatedJWT(String idToken, String state, String origin, HttpSession session) {
        DecodedJWT jwt;
        ToolDeployment toolDeployment = null;
        /**
         * 1. The Tool MUST Validate the signature of the ID Token according to JSON Web Signature [RFC7515],
         * Section 5.2 using the Public Key from the Platform;
         */
        try {
            jwt = JWT.decode(idToken);
            for (int i = 0; i < jwt.getAudience().size(); i++) {
                Optional<ToolDeployment> optionalToolDeployment = this.toolDeploymentRepository.findByClientId(
                        jwt.getAudience().get(i)
                );
                if (optionalToolDeployment.isPresent()) {
                    toolDeployment = optionalToolDeployment.get();
                    break;
                }
            }
            if (null == toolDeployment) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Audience invalid");
            }
            UrlJwkProvider provider = new UrlJwkProvider(new URL(toolDeployment.getJwksUri()));
            logger.debug("Keys from URL '{}': {}", toolDeployment.getJwksUri(), provider.getAll());
            logger.debug("Retrieving key of id '{}'", jwt.getKeyId());
            Jwk jwk = provider.get(jwt.getKeyId());
            Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
            JWTVerifier verifier = JWT.require(algorithm).build();
            verifier.verify(idToken);

        } catch (Throwable ex) {
            logger.warn(ex.toString());
            logger.debug(ExceptionUtils.getStackTrace(ex));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token");
        }

        /**
         * 2. The Issuer Identifier for the Platform MUST exactly match the value of the iss (Issuer) Claim
         * (therefore the Tool MUST previously have been made aware of this identifier);
         */
        if (!jwt.getIssuer().equals(origin)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Issuer invalid");
        }

        /** 3. The Tool MUST validate that the aud (audience) Claim contains its client_id value registered as an
         * audience with the Issuer identified by the iss (Issuer) Claim. The aud (audience) Claim MAY contain an array
         * with more than one element. The Tool MUST reject the ID Token if it does not list the client_id as a valid
         * audience, or if it contains additional audiences not trusted by the Tool. The request message will be
         * rejected with a HTTP code of 401;
         */
        final List<String> audiences = jwt.getAudience();
        if (!audiences.contains(toolDeployment.getClientId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Audience invalid");
        }

        /**
         * 4. If the ID Token contains multiple audiences, the Tool SHOULD verify that an azp Claim is present;
         */
        if (audiences.size() > 1) {
            if (jwt.getClaim("azp").isMissing() || jwt.getClaim("azp").isNull()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Azp claim is not present");
            }
        }

        /**
         * 5. If an azp (authorized party) Claim is present, the Tool SHOULD verify that its client_id is the Claim's
         * value
         */
        if (!jwt.getClaim("azp").isMissing() && !jwt.getClaim("azp").isNull()) {
            if (!toolDeployment.getClientId().equals(jwt.getClaim("azp").asString())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Azp invalid");
            }
        }


        /**
         * 6. The alg value SHOULD be the default of RS256 or the algorithm sent by the Tool in
         * the id_token_signed_response_alg parameter during its registration. Use of algorithms other that RS256 will
         * limit interoperability
         *
         */
        if (!jwt.getAlgorithm().equals("RS256")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported algorithm");
        }

        /**
         * 7. The current time MUST be before the time represented by the exp Claim;
         */
        if (jwt.getExpiresAt().before(new Date())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expired");
        }

        /**
         * 8. The Tool MAY use the iat Claim to reject tokens that were issued too far away from the current time,
         * limiting the amount of time that it needs to store nonces used to prevent attacks.
         * The Tool MAY define its own acceptable time range;
         *
         */
        if (jwt.getIssuedAt().before(new Date(
                (new Date()).getTime() - TimeUnit.HOURS.toMillis(NONCE_EXPIRATION_HOURS)
        ))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Issued too early");
        }

        /**
         * 9. The ID Token MUST contain a nonce Claim. The Tool SHOULD verify that it has not yet received this nonce
         * value (within a Tool-defined time window), in order to help prevent replay attacks. The Tool MAY define its
         * own precise method for detecting replay attacks.
         *
         */
        if (jwt.getClaim("nonce").isMissing() || jwt.getClaim("nonce").isNull()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nonce not present");
        }
        String nonce = jwt.getClaim("nonce").asString();
        if (Boolean.TRUE.equals(this.noncesCache.getIfPresent(nonce))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nonce already used");
        }
        this.noncesCache.put(nonce, true);

        if (!state.equals(session.getAttribute(STATE_ATTRIBUTE))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrong state");
        }
        return jwt;
    }
}
