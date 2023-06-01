package fr.centralesupelec.thuv.lti.model;

import lombok.Builder;
import lombok.Data;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@Entity
@Table(name = "tool_deployments")
public class ToolDeployment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    private String clientId;

    public ToolDeployment(Long id, String clientId, String iss, String jwksUri, String authorizationEndpoint) {
        this.id = id;
        this.clientId = clientId;
        this.iss = iss;
        this.jwksUri = jwksUri;
        this.authorizationEndpoint = authorizationEndpoint;
    }

    @NotBlank
    private String iss;
    @NotBlank
    private String jwksUri;

    public ToolDeployment() {
    }

    @NotBlank
    private String authorizationEndpoint;

}
