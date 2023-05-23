package fr.centralesupelec.thuv.scale_up.dtos.terraform_state;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.Map;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TerraformInstance {
    private String indexKey;
    private Attributes attributes;

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Attributes {
        private String region;
        private String powerState;
        private String flavorName;
        private String accessIpV4;
        private Map<String, String> metadata;
    }
}
