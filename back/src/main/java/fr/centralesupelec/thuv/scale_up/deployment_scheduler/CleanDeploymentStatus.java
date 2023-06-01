package fr.centralesupelec.thuv.scale_up.deployment_scheduler;

import fr.centralesupelec.thuv.scale_up.repository.DeploymentStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CleanDeploymentStatus {
    private static final long DAY_KEEP_IN_DATABASE = 7;
    private final DeploymentStatusRepository deploymentStatusRepository;

    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void cleanDeploymentStatus() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(DAY_KEEP_IN_DATABASE);
        deploymentStatusRepository.deleteAll(
                deploymentStatusRepository.findByCreatedOnBefore(oneWeekAgo)
        );
    }
}
