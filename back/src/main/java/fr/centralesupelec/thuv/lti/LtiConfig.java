package fr.centralesupelec.thuv.lti;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

@Configuration
public class LtiConfig {
    public static final Integer NONCE_EXPIRATION_HOURS = 24;
    public static final Integer NONCE_MAX_COUNT = 1000;
    public static final String STATE_ATTRIBUTE = "lti_state";
    public static final String DEEP_LINKING_ATTRIBUTE = "lti_deep_linking";
    public static final String REGISTER_PATH = "/lti/register";
    public static final String INIT_PATH = "/lti/init";
    public static final String LAUNCH_PATH = "/lti/launch";
    public static final String DEEP_LINKING_FRONT_PATH = "/lti/deep-linking";
    public static final String DEEP_LINKING_BACK_PATH = "/lti/deep-linking";
    public static final String JWKS_PATH = "/.well-known/jwks.json";

    public static final class Claims {
        public static final String MESSAGE_TYPE = "https://purl.imsglobal.org/spec/lti/claim/message_type";
        public static final String VERSION = "https://purl.imsglobal.org/spec/lti/claim/version";
        public static final String DEPLOYMENT_ID = "https://purl.imsglobal.org/spec/lti/claim/deployment_id";
        public static final String CONTENT_ITEMS = "https://purl.imsglobal.org/spec/lti-dl/claim/content_items";
        public static final String DATA = "https://purl.imsglobal.org/spec/lti/claim/data";
        public static final String ERROR_MESSAGE = "https://purl.imsglobal.org/spec/lti/claim/errormsg";
    }

    @Value("${lti.base-path}")
    private String basePath;

    @Bean
    public String basePath() {
        return this.basePath;
    }

    @Bean
    public String baseDomain() throws MalformedURLException {
        URL url = new URL(this.basePath);
        return url.getPort() == -1
                ? String.format("%s", url.getHost())
                : String.format("%s:%s", url.getHost(), url.getPort());
    }

    @Bean
    public String baseUrl() throws MalformedURLException {
        URL url = new URL(this.basePath);
        return url.getPort() == -1
                ? String.format("%s://%s", url.getProtocol(), url.getHost())
                : String.format("%s://%s:%s", url.getProtocol(), url.getHost(), url.getPort());
    }

    @Bean
    public Cache<String, Boolean> noncesCache() {
        return CacheBuilder
                .newBuilder()
                .maximumSize(LtiConfig.NONCE_MAX_COUNT)
                .expireAfterWrite(LtiConfig.NONCE_EXPIRATION_HOURS, TimeUnit.HOURS)
                .build();
    }

    @Bean
    public RestTemplate ltiRestTemplate() {
        return new RestTemplate();
    }
}
