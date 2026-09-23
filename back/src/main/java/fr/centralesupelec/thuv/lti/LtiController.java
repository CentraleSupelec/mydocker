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
import org.springframework.dao.DataIntegrityViolationException;
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

        String clientId = this.ltiService.registerTool(registrationToken, openIdConfiguration);

        // The guard cannot run before registerTool, because that call is what yields the client id,
        // and the pair identifies a deployment: an issuer carries one row per espace de cours, so a
        // second registration on a platform we already know is the normal case, not a duplicate.
        // Refusing here leaves the registration the platform just recorded on its own side with no
        // counterpart. That is the accepted cost of never rewriting an existing trust record: the
        // configuration URL is caller-supplied and the client id comes back from the endpoint it
        // names, so overwriting a stored row on a repeated pair would let one caller repoint another
        // platform's jwks_uri.
        if (this.toolDeploymentRepository.findByIssAndClientId(openIdConfiguration.getIssuer(), clientId).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A deployment for this platform and client already exists"
            );
        }

        ToolDeployment newTool = ToolDeployment
                .builder()
                .iss(openIdConfiguration.getIssuer())
                .clientId(clientId)
                .jwksUri(openIdConfiguration.getJwksUri())
                .authorizationEndpoint(openIdConfiguration.getAuthorizationEndpoint())
                .build();

        try {
            this.toolDeploymentRepository.saveAndFlush(newTool);
        } catch (DataIntegrityViolationException ex) {
            // Two registrations of the same pair can both pass the check above before either
            // writes. The unique constraint is what keeps the data correct; this turns the
            // resulting failure into the same answer the check gives, rather than a 500.
            logger.warn(
                    "Concurrent registration of an existing deployment for issuer '{}'",
                    openIdConfiguration.getIssuer()
            );
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A deployment for this platform and client already exists"
            );
        }
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
        // The pair identifies the deployment. Resolving on the client id alone would let a request
        // name one issuer and a client id registered under another, and the redirect would then be
        // built from the second platform's authorization endpoint, carrying this platform's
        // login_hint and lti_message_hint to it.
        if (clientId.isPresent()) {
            optionalToolDeployment = this.toolDeploymentRepository.findByIssAndClientId(iss, clientId.get());
        } else {
            // client_id is optional in the protocol, so the single-integration platforms keep
            // working. An issuer with several espace de cours integrations cannot be resolved this
            // way, and guessing one of them would send the hints to the wrong course.
            List<ToolDeployment> deploymentsForIssuer = this.toolDeploymentRepository.findAllByIss(iss);
            if (1 < deploymentsForIssuer.size()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format("client_id is required for platform %s", iss)
                );
            }
            optionalToolDeployment = deploymentsForIssuer.stream().findFirst();
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

        // The email claim is the only identity this launch carries: it is both the lookup key and
        // the stored address. Without it the upsert resolves nothing, creates a row with a null
        // username and a null email, and saves it, so a token with no email would write a junk
        // account before the session is minted.
        String email = jwt.getClaim("email").asString();
        if (null == email || email.isBlank()) {
            throw rejectLaunch("email-missing", "Email not present");
        }

        User user = myUserDetailsService.upsertUserFromUntrustedOrigin(
                null,
                email,
                jwt.getClaim("given_name").asString(),
                jwt.getClaim("family_name").asString(),
                TokenOrigin.LTI
        );

        List<String> lmsRoles = jwt.getClaim(Claims.ROLES).asList(String.class);
        if (null != lmsRoles && lmsRoles.contains(Roles.INSTRUCTOR)) {
            // Any registered platform may assert Instructor, as it could before this hardening.
            // Registration is administrator-only and the launch issuer is checked against the
            // registered platform, so registration is the trust boundary. A second allow-list here
            // would silently stop teacher promotion on every platform already registered.
            myUserDetailsService.ensureTeacher(user);
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
