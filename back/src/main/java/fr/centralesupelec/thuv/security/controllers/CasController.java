package fr.centralesupelec.thuv.security.controllers;

import fr.centralesupelec.thuv.activity_logging.model.LogAction;
import fr.centralesupelec.thuv.activity_logging.services.ActivityLogger;
import fr.centralesupelec.thuv.exception.CasAuthenticationException;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.model.cas.AuthenticationSuccessType;
import fr.centralesupelec.thuv.model.cas.ServiceResponseType;
import fr.centralesupelec.thuv.security.JwtTokenProvider;
import fr.centralesupelec.thuv.security.MyUserDetailsService;
import fr.centralesupelec.thuv.security.dtos.TokenOrigin;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/auth/cas")
public class CasController {
    private static final Logger logger = LoggerFactory.getLogger(CasController.class);
    private final RestTemplate restTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final MyUserDetailsService myUserDetailsService;

    @Value("${app.cas.rscUrl}")
    private String rscURL;

    @Value("${app.cas.service}")
    private String defaultServiceURL;

    private Unmarshaller unmarshaller;
    private final ActivityLogger activityLogger;

    @Autowired
    public CasController(
            JwtTokenProvider jwtTokenProvider,
            MyUserDetailsService myUserDetailsService,
            ActivityLogger activityLogger
    ) {
        this.myUserDetailsService = myUserDetailsService;
        this.activityLogger = activityLogger;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance("fr.centralesupelec.thuv.model.cas");
            unmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            Sentry.captureException(e);
            logger.error("Fail to load JAXB Context " + e);
        }
        restTemplate = new RestTemplate();
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping(value = "/{token}", produces = "application/json")
    public ResponseEntity<?> login(
            @PathVariable String token,
            @RequestParam String serviceURL
    ) {
        logger.debug("Token : " + token);

        UriComponentsBuilder tokenVerifierBuilder = UriComponentsBuilder.fromHttpUrl(rscURL);

        if (null == serviceURL || serviceURL.isEmpty()) {
            serviceURL = defaultServiceURL;
        } else if (!serviceURL.startsWith(defaultServiceURL)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Your service url does not start with " + defaultServiceURL + " given " + serviceURL);
        }

        tokenVerifierBuilder.queryParam("service", serviceURL);
        tokenVerifierBuilder.queryParam("ticket", token);

        String url = tokenVerifierBuilder.build().encode().toUriString();

        logger.debug("Forge URL: " + url);

        try {
            // Verify the CAS ticket
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            logger.debug("Request status: " + response.getStatusCode());
            String xml = response.getBody();

            // Unmarshal xml response
            JAXBElement<ServiceResponseType> responseElement =
                    (JAXBElement<ServiceResponseType>) unmarshaller.unmarshal(new StringReader(xml));
            AuthenticationSuccessType auth = responseElement.getValue().getAuthenticationSuccess();

            if (auth == null) {
                throw new CasAuthenticationException(
                        "An error occurred with ticket: " + token + ", Cas response: " + xml
                );
            }

            String email = auth.getUser();
            String name;
            String lastName;
            try {
                String simpleName = auth.getAttributes().getSimpleName();
                List<String> splitSimpleName = new ArrayList<>(Arrays.asList(simpleName.split(" ")));
                name = splitSimpleName.remove(0);
                lastName = String.join(" ", splitSimpleName);
            } catch (NullPointerException ex) {
                Sentry.captureException(ex);
                name = "DefaultName";
                lastName = "DefaultName";
            }

            // FindorCreate user

            User user = myUserDetailsService.upsertUser(null, email, name, lastName);

            String jwtToken = jwtTokenProvider
                    .generateToken(
                            user.getUsername(), user.getEmail(), TokenOrigin.CAS
                    );
            logger.debug("JWT token: " + jwtToken);
            activityLogger.log(LogAction.USER_LOGIN_CAS, user);
            return ResponseEntity.ok(jwtToken);
        } catch (Exception ex) {
            Sentry.captureException(ex);
            logger.error("An error occured during ticket verification " + ex);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occured during ticket verification ... :(");
        }
    }
}
