package fr.centralesupelec.thuv.scale_up.services;

import fr.centralesupelec.thuv.scale_up.model.CleanDeployment;
import fr.centralesupelec.thuv.scale_up.model.Deployment;
import fr.centralesupelec.thuv.scale_up.model.LaunchDeployment;
import fr.centralesupelec.thuv.scale_up.repository.DeploymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteDeploymentService {
    private final DeploymentRepository deploymentRepository;

    public void deleteDeployment(Deployment deployment) {
        if (deployment instanceof CleanDeployment) {
            deleteDeployment((CleanDeployment) deployment);
        }

        if (deployment instanceof LaunchDeployment) {
            deleteDeployment((LaunchDeployment) deployment);
        }
    }

    private void deleteDeployment(LaunchDeployment launchDeployment) {
        launchDeployment.getSessionsToLaunch().forEach(
                s -> {
                    s.setLaunchDeployment(null);
                    if (s.getCleanDeployment() != null) {
                        deleteDeployment(s.getCleanDeployment());
                    }
                }
        );
        launchDeployment.getSessionsToLaunch().clear();
        deploymentRepository.delete(launchDeployment);
    }

    private void deleteDeployment(CleanDeployment cleanDeployment) {
        cleanDeployment.getWorkersToClean().forEach(
                w -> w.setCleanDeployment(null)
        );
        cleanDeployment.getWorkersToClean().clear();
        cleanDeployment.getSessionsToClean().forEach(
                s -> s.setCleanDeployment(null)
        );
        cleanDeployment.getSessionsToClean().clear();
        deploymentRepository.delete(cleanDeployment);
    }
}
