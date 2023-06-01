package fr.centralesupelec.thuv.mail;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Component
@Validated
@ConfigurationProperties(prefix = "notification.mails")
@Data
public class MailParameterConfiguration {
    @NotNull
    private String[] to = new String[]{};
    @NotEmpty
    private String from;
}
