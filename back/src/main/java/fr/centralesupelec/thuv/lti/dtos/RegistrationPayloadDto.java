package fr.centralesupelec.thuv.lti.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RegistrationPayloadDto {
    public String clientName;
    public String initiateLoginUri;
    public List<String> redirectUris;
    public String jwksUri;
    public String scope;
    @JsonProperty("https://purl.imsglobal.org/spec/lti-tool-configuration")
    public HttpsPurlImsglobalOrgSpecLtiToolConfigurationDto httpsPurlImsglobalOrgSpecLtiToolConfiguration;
    public List<String> responseTypes;
    public List<String> grantTypes;
    public String tokenEndpointAuthMethod;
    public String idTokenSignedResponseAlg;
    public String logoUri;

    @Data
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class HttpsPurlImsglobalOrgSpecLtiToolConfigurationDto {
        public String domain;
        public String targetLinkUri;
        public Map<String, String> customParameters;
        public List<String> claims;
        public List<Object> messages;
        public String description;
    }
}

