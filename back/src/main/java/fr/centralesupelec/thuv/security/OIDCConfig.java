package fr.centralesupelec.thuv.security;

import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Configuration
public class OIDCConfig {
    private static final Logger logger = LoggerFactory.getLogger(OIDCConfig.class);
    private static final int OIDC_WELL_KNOWN_CACHE_SIZE = 10;
    private static final int OIDC_WELL_KNOWN_CACHE_EXPIRATION_HOURS = 24;
    public static final int JWKS_CACHE_SIZE = 10;
    public static final int JWKS_CACHE_EXPIRATION_HOURS = 24;
    public static final String JWK_BEAN_NAME = "jwkProvider";

    @Bean
    public Cache<String, String> wellKnownCache() {
        return CacheBuilder
                .newBuilder()
                .maximumSize(OIDCConfig.OIDC_WELL_KNOWN_CACHE_SIZE)
                .expireAfterWrite(OIDCConfig.OIDC_WELL_KNOWN_CACHE_EXPIRATION_HOURS, TimeUnit.HOURS)
                .build();
    }

    @Bean(name = OIDCConfig.JWK_BEAN_NAME)
    public JwkProvider jwkProvider(
            @Value("${oidc.issuer}") String issuer,
            Cache<String, String> wellKnownCache,
            RestTemplate restTemplate
    ) throws MalformedURLException, ExecutionException {
        if (StringUtils.isBlank(issuer)) {
            logger.debug("Skipping OIDC Provider initialization");
            return null;
        }
        logger.debug("Initializing OIDC Provider");
        String jwksUriValue = wellKnownCache
                .get(issuer, () -> {
                    String responseBody = restTemplate.getForObject(
                            String.format("%s/%s", issuer, ".well-known/openid-configuration"),
                            String.class
                    );
                    ObjectMapper objectMapper = new ObjectMapper();
                    return objectMapper.readTree(responseBody).get("jwks_uri").asText();
                });
        return new JwkProviderBuilder(new URL(jwksUriValue))
                .cached(JWKS_CACHE_SIZE, JWKS_CACHE_EXPIRATION_HOURS, TimeUnit.HOURS)
                .build();
    }
}
