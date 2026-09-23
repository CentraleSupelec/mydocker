package fr.centralesupelec.thuv.lti;

import static fr.centralesupelec.thuv.lti.LtiConfig.*;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.cache.Cache;
import fr.centralesupelec.thuv.activity_logging.model.LogAction;
import fr.centralesupelec.thuv.activity_logging.services.ActivityLogger;
import fr.centralesupelec.thuv.lti.dtos.DeepLinkingPayloadDto;
import fr.centralesupelec.thuv.lti.dtos.OpenIdConfigurationDto;
import fr.centralesupelec.thuv.lti.model.ToolDeployment;
import fr.centralesupelec.thuv.lti.repository.ToolDeploymentRepository;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.security.JwtTokenProvider;
import fr.centralesupelec.thuv.security.MyUserDetailsService;
import fr.centralesupelec.thuv.security.dtos.TokenOrigin;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import jakarta.servlet.http.HttpSession;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class LtiController {
    private static final Logger logger = LoggerFactory.getLogger(LtiController.class);
    private final ToolDeploymentRepository toolDeploymentRepository;
    private final LtiService ltiService;
    private final Cache<String, Boolean> noncesCache;
    private final RestTemplate ltiRestTemplate;
    private final MyUserDetailsService myUserDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final String basePath;
    private final String baseUrl;
    private final LtiJwtService ltiJwtService;
    private final ActivityLogger activityLogger;
    private final LtiTrustPolicy ltiTrustPolicy;


    @GetMapping(REGISTER_PATH)
    public void register(
            @RequestParam("openid_configuration") String openidConfiguration,
            @RequestParam("registration_token") String registrationToken
    ) throws JsonProcessingException {
        OpenIdConfigurationDto openIdConfiguration = ltiRestTemplate.getForObject(
                openidConfiguration, OpenIdConfigurationDto.class
        );
        assert openIdConfiguration != null;
        logger.debug("Fetched openIdConfiguration {}", openIdConfiguration);

        if (this.toolDeploymentRepository.findByIss(openIdConfiguration.getIssuer()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A deployment for this platform already exists");
        }

        ToolDeployment newTool = ToolDeployment
                .builder()
                .iss(openIdConfiguration.getIssuer())
                .jwksUri(openIdConfiguration.getJwksUri())
                .authorizationEndpoint(openIdConfiguration.getAuthorizationEndpoint())
                .build();


        String clientId = this.ltiService.registerTool(registrationToken, openIdConfiguration);
        newTool.setClientId(clientId);
        this.toolDeploymentRepository.saveAndFlush(newTool);
    }

    @GetMapping(value = JWKS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    public String jwks() {
        return this.ltiJwtService.getJwks();
    }

    @PostMapping(value = INIT_PATH)
    public ResponseEntity<String> init(
            @RequestParam("iss") String iss,
            @RequestParam("target_link_uri") String targetLinkUri,
            @RequestParam("login_hint") String loginHint,
            @RequestParam(value = "lti_message_hint", required = false) Optional<String> ltiMessageHint,
            @RequestParam(value = "client_id", required = false) Optional<String> clientId,
            @RequestParam(value = "lti_deployment_id", required = false) Optional<String> ltiDeploymentId,
            HttpSession session
    ) {
        Optional<ToolDeployment> optionalToolDeployment;
        // LTI protocol specifies to use iss because client_id is not required, but using client_id enables multiple
        // tool deployments on the same platform.
        if (clientId.isPresent()) {
            optionalToolDeployment = this.toolDeploymentRepository.findByClientId(clientId.get());
        } else {
            optionalToolDeployment = this.toolDeploymentRepository.findByIss(iss);
        }
        if (!optionalToolDeployment.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Platform %s not configured", iss));
        }
        ToolDeployment toolDeployment = optionalToolDeployment.get();

        String state = UUID.randomUUID().toString();
        session.setAttribute(STATE_ATTRIBUTE, state);
        String nonce = UUID.randomUUID().toString();
        this.noncesCache.put(nonce, false);

        String redirectUri = String.format("%s%s", basePath, LAUNCH_PATH);
        String url = UriComponentsBuilder.fromHttpUrl(toolDeployment.getAuthorizationEndpoint())
                .queryParam("scope", "openid")// OIDC Scope.
                .queryParam("response_type", "id_token")// OIDC response is always an id token.
                .queryParam("response_mode", "form_post")// OIDC response is always a form post.
                .queryParam("prompt", "none")// Don"t prompt user on redirect.
                .queryParam("client_id", toolDeployment.getClientId())// Registered client id.
                .queryParam("redirect_uri", redirectUri)// URL to return to after login.
                .queryParam("state", state)// State to identify browser session.
                .queryParam("nonce", nonce)// Prevent replay attacks.
                .queryParam("login_hint", loginHint)// Login hint to identify platform session
                .queryParam("lti_message_hint", ltiMessageHint)// LTI Message hint to identify platform session
                .build()
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, url);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @PostMapping(LAUNCH_PATH)
    public ResponseEntity<String> launch(
            @RequestParam("id_token") String idToken,
            @RequestParam("state") String state,
            HttpSession session
    ) {
        DecodedJWT jwt = this.ltiService.getValidatedJWT(idToken, state, session);

        // Everything the launch needs is checked before anything is written. A malformed or
        // unsupported launch used to fail after the profile had been updated, a teacher role
        // possibly granted and a successful login recorded.
        String messageType = jwt.getClaim(Claims.MESSAGE_TYPE).asString();
        String targetUri = jwt.getClaim(Claims.TARGET_LINK_URI).asString();
        if (null == messageType) {
            throw rejectLaunch("message-type-missing", "Message type not present");
        }
        if (!MessageTypes.RESOURCE_LINK.equals(messageType) && !MessageTypes.DEEP_LINKING.equals(messageType)) {
            throw rejectLaunch("message-type", String.format("Unsupported type : %s", messageType));
        }
        if (MessageTypes.RESOURCE_LINK.equals(messageType)) {
            if (null == targetUri) {
                throw rejectLaunch("target-link-uri-missing", "Target link URI not present");
            }
            // Parsed here, not at redirect time: by then the profile, a possible teacher grant and
            // the login record are already written. UriComponentsBuilder accepts values that are
            // not absolute http URLs, so the scheme is checked rather than the parse alone.
            try {
                URI parsed = new URI(targetUri);
                if (!parsed.isAbsolute()
                        || null == parsed.getHost()
                        || !List.of("http", "https").contains(parsed.getScheme())) {
                    throw rejectLaunch("target-link-uri-malformed", "Target link URI invalid");
                }
            } catch (URISyntaxException ex) {
                throw rejectLaunch("target-link-uri-malformed", "Target link URI invalid");
            }
        }

        User user = myUserDetailsService.upsertUserFromUntrustedOrigin(
                null,
                jwt.getClaim("email").asString(),
                jwt.getClaim("given_name").asString(),
                jwt.getClaim("family_name").asString(),
                TokenOrigin.LTI
        );

        List<String> lmsRoles = jwt.getClaim(Claims.ROLES).asList(String.class);
        if (null != lmsRoles && lmsRoles.contains(Roles.INSTRUCTOR)) {
            // Registered is not trusted: a platform we do not operate does not get to say who
            // teaches here.
            if (this.ltiTrustPolicy.mayGrantTeacherRole(jwt.getIssuer())) {
                myUserDetailsService.ensureTeacher(user);
            } else {
                logger.warn("Ignored Instructor claim from untrusted issuer '{}'", jwt.getIssuer());
            }
        }

        String localToken = jwtTokenProvider
                .generateToken(
                        user.getUsername(), user.getEmail(), TokenOrigin.LTI
                );

        activityLogger.log(LogAction.USER_LOGIN_LTI, user);

        if (MessageTypes.DEEP_LINKING.equals(messageType)) {
            return redirectDeepLinking(jwt, localToken, session);
        }
        return redirectResourceLink(targetUri, localToken);
    }

    private ResponseStatusException rejectLaunch(String check, String message) {
        logger.warn("LTI launch rejected by check '{}'", check);
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseEntity<String> redirectResourceLink(String targetUri, String localToken) {
        String url = UriComponentsBuilder.fromHttpUrl(targetUri)
                .queryParam("idtoken", localToken)
                .build()
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, url);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    private ResponseEntity<String> redirectDeepLinking(DecodedJWT jwt, String localToken, HttpSession session) {
        String targetUri = String.format("%s%s", this.baseUrl, DEEP_LINKING_FRONT_PATH);
        session.setAttribute(DEEP_LINKING_ATTRIBUTE, jwt);

        String url = UriComponentsBuilder.fromHttpUrl(targetUri)
                .queryParam("idtoken", localToken)
                .build()
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, url);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @PostMapping(DEEP_LINKING_BACK_PATH)
    @ResponseBody
    public Map<String, String> createDeepLinkingJWT(
            HttpSession session,
            @RequestBody JsonNode body
            ) {
        DecodedJWT requestJwt = (DecodedJWT) session.getAttribute(DEEP_LINKING_ATTRIBUTE);
        DeepLinkingPayloadDto payload = DeepLinkingPayloadDto
                .builder()
                .aud(requestJwt.getIssuer())
                .iss(requestJwt.getAudience().get(0))
                .jti(requestJwt.getClaim("nonce").asString())
                .nonce(requestJwt.getClaim("nonce").asString())
                .sub(requestJwt.getAudience().get(0))
                .version(requestJwt.getClaim(Claims.VERSION).asString())
                .deploymentId(requestJwt.getClaim(Claims.DEPLOYMENT_ID).asString())
                .contentItems(List.of(
                        DeepLinkingPayloadDto.ContentItemDto
                                .builder()
                                .type("ltiResourceLink")
                                .url(body.get("url").asText())
                                .window(DeepLinkingPayloadDto.Window.builder().build())
                                .build()
                ))
                .build()
            ;
        return Map.ofEntries(
                Map.entry("JWT", this.ltiJwtService.signPayload(payload)),
                Map.entry(
                        "deepLinkingUrl",
                        requestJwt
                                .getClaim("https://purl.imsglobal.org/spec/lti-dl/claim/deep_linking_settings")
                                .asMap()
                                .get("deep_link_return_url")
                                .toString()
                )
            );
    }
}
