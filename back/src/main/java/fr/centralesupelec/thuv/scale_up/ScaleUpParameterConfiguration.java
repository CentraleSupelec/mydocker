package fr.centralesupelec.thuv.scale_up;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotEmpty;

@Component
@Validated
@ConfigurationProperties(prefix = "scale-up")
@Data
public class ScaleUpParameterConfiguration {
    @NotEmpty
    private String terraformStateUrl;
}
