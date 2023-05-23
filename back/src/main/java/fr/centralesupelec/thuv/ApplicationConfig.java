package fr.centralesupelec.thuv;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;

@Configuration
public class ApplicationConfig {
    @Bean
    public ZoneId zoneId() {
        return ZoneId.of("Europe/Paris");
    }
}
