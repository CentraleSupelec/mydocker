package fr.centralesupelec.thuv.lti.dtos;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class OpenIdConfigurationDto {
    String issuer;
    String tokenEndpoint;
    String jwksUri;
    String authorizationEndpoint;
    String registrationEndpoint;
}
