package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import fr.centralesupelec.thuv.model.CourseSession;
import fr.centralesupelec.thuv.model.User;

@Service
public class SlurmApiService {

    private static final Logger logger = LoggerFactory.getLogger(SlurmApiService.class);
    private static final int CONNECT_TIMEOUT_MILLIS = 3000;
    private static final int READ_TIMEOUT_MILLIS = 5000;

    private final RestTemplate restTemplate;

    public SlurmApiService() {
        // Finite timeouts. This call sits in the container-launch critical path,
        // and a default RestTemplate waits forever, so a slow or hung gateway
        // stalls every launch on a Slurm course. Values are
        // conservative; tune per environment.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        factory.setReadTimeout(READ_TIMEOUT_MILLIS);
        this.restTemplate = new RestTemplate(factory);
    }

    public SlurmApiContainerParams getSlurmApiContainerParams(CourseSession courseSession, User user) {
        if (courseSession.getCourse().getSlurmCluster() != null) {
            String apiEndpoint;
            String secretKey;
            try {
                // Initializes the lazy cluster, which decrypts its secret.
                apiEndpoint = courseSession.getCourse().getSlurmCluster().getApiEndpoint();
                secretKey = courseSession.getCourse().getSlurmCluster().getSecretKey();
            } catch (RuntimeException e) {
                logger.error("Could not load Slurm cluster secret for course {}; launching without Slurm: {}",
                        courseSession.getCourse().getId(), e.getMessage(), e);
                return null;
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(secretKey);
            try {
                logger.debug("Fetching Slurm API {}", apiEndpoint);
                String url = String.format(
                        "%s/api_key?user=%s&session_id=%s",
                        apiEndpoint, user.getId(),
                        courseSession.getId()
                );
                ResponseEntity<String> response = restTemplate.exchange(
                        url, HttpMethod.GET, new HttpEntity<>(headers),
                        String.class);
                String containerApiKey = response.getBody();
                if (response.getStatusCode().is2xxSuccessful() && !StringUtils.isEmpty(containerApiKey)) {
                    SlurmApiContainerParams.Builder slurmApiContainerParamsBuilder = SlurmApiContainerParams
                            .newBuilder()
                            .setEndpoint(apiEndpoint)
                            .setKey(containerApiKey);
                    return slurmApiContainerParamsBuilder.build();
                } else {
                    logger.error("Slurm API did not return the expected answer {}", response);
                }
            } catch (RestClientException e) {
                // A rejected certificate and a dead gateway both end up on the
                // fail-open path below, where the only visible effect is that
                // Slurm is quietly absent from the launch. Name the TLS case
                // explicitly so it is not mistaken for the gateway being down:
                // the usual cause is a self-signed gateway certificate that the
                // back-end host does not trust, or one issued without a
                // subjectAltName, which Java rejects outright.
                if (hasCause(e, javax.net.ssl.SSLException.class)
                        || hasCause(e, java.security.cert.CertificateException.class)) {
                    logger.error(
                            "Slurm gateway TLS validation failed for {} - the certificate is not trusted "
                                    + "by this host or has no matching subjectAltName. Slurm will be "
                                    + "silently omitted from launches for this course until it is fixed: {}",
                            apiEndpoint, e.getMessage(), e);
                } else {
                    logger.error(String.format("Could not fetch Slurm API : %s", e.getMessage()), e);
                }
            }
        }
        // NOTE the fail-open below means a TLS misconfiguration is not visible
        // to the student, only in this log.
        // DELIBERATE FAIL-OPEN: on any gateway error (timeout, non-2xx, empty
        // body) we return null and the caller launches the container WITHOUT
        // Slurm wiring, silently. This is acceptable for a preprod trial but is
        // a decision the owner must confirm before production: to fail closed
        // instead, throw here so a launch is blocked when a Slurm course cannot
        // reach its gateway.
        return null;
    }

    private static boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        for (Throwable t = throwable; t != null; t = t.getCause()) {
            if (type.isInstance(t)) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }
}
