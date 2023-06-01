package fr.centralesupelec.thuv.lti.dtos;

import static fr.centralesupelec.thuv.lti.LtiConfig.Claims.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DeepLinkingPayloadDto {
    public String aud;
    public String iss;
    public String nonce;
    public String jti;
    public String sub;
    @JsonProperty(MESSAGE_TYPE)
    public final String messageType = "LtiDeepLinkingResponse";
    @JsonProperty(VERSION)
    public String version;
    @JsonProperty(DEPLOYMENT_ID)
    public String deploymentId;
    @JsonProperty(ERROR_MESSAGE)
    public String errorMsg;
    @JsonProperty(CONTENT_ITEMS)
    public List<ContentItemDto> contentItems;
    @JsonProperty(DATA)
    public Object data;

    @Data
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class ContentItemDto {
        public String type;
        public String url;
        public Window window;
    }

    @Data
    @Builder
    public static class Window {
        public final String targetName = "mydocker-win";
    }
}

