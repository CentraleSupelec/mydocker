package fr.centralesupelec.thuv.scale_up.deployment_scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "deployment_enabled", havingValue = "true")
public class DeploymentScheduler {
    private final DeploymentLauncher deploymentLauncher;

    @Scheduled(fixedDelayString = "${deployment_delay_in_milliseconds}", initialDelay = 0)
    public void launchDeployment() throws JsonProcessingException {
        this.deploymentLauncher.launchDeployment();
    }
}
