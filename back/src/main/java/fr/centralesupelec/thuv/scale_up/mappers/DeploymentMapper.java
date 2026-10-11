package fr.centralesupelec.thuv.scale_up.mappers;

import fr.centralesupelec.thuv.mappers.SessionWithCourseMapper;
import fr.centralesupelec.thuv.repository.CourseSessionRepository;
import fr.centralesupelec.thuv.scale_up.dtos.*;
import fr.centralesupelec.thuv.scale_up.model.CleanDeployment;
import fr.centralesupelec.thuv.scale_up.model.Deployment;
import fr.centralesupelec.thuv.scale_up.model.LaunchDeployment;
import fr.centralesupelec.thuv.scale_up.repository.OVHRegionWorkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeploymentMapper {
    private final ZoneId zoneId;
    private final OVHRegionWorkerMapper ovhRegionWorkerMapper;
    private final OVHRegionWorkerWithSessionMapper ovhRegionWorkerWithSessionMapper;
    private final CourseSessionRepository courseSessionRepository;
    private final OVHRegionWorkerRepository ovhRegionWorkerRepository;
    private final SessionWithCourseMapper sessionWithCourseMapper;

    public DeploymentDto convertToDto(Deployment deployment) {
        DeploymentDto deploymentDto;
        if (deployment instanceof LaunchDeployment) {
            deploymentDto = new LaunchDeploymentDto();
        } else {
            deploymentDto = new CleanDeploymentDto();
        }
        deploymentDto
                .setDescription(
                        deployment.getDescription()
                )
                .setUpdatedOn(
                        deployment.getUpdatedOn().atZone(zoneId).toInstant().toEpochMilli()
                )
                .setId(
                        deployment.getId()
                )
                .setStartDateTime(
                        deployment.getStartDateTime().atZone(zoneId).toInstant().toEpochMilli()
                );
        if (deployment instanceof LaunchDeployment) {
            deploymentDto
                    .setWorkers(
                            ((LaunchDeployment) deployment).getWorkersToLaunch()
                                    .stream()
                                    .map(ovhRegionWorkerWithSessionMapper::convertToDto)
                                    .collect(Collectors.toList())
                    )
                    .setSessions(
                            ((LaunchDeployment) deployment).getSessionsToLaunch()
                                    .stream()
                                    .map(sessionWithCourseMapper::convertToDto)
                                    .collect(Collectors.toList())
                    );
        } else {
            deploymentDto
                    .setWorkers(
                            ((CleanDeployment) deployment).getWorkersToClean()
                                    .stream()
                                    .map(ovhRegionWorkerWithSessionMapper::convertToDto)
                                    .collect(Collectors.toList())
                    )
                    .setSessions(
                            ((CleanDeployment) deployment).getSessionsToClean()
                                    .stream()
                                    .map(sessionWithCourseMapper::convertToDto)
                                    .collect(Collectors.toList())
                    );
        }
        return deploymentDto;
    }

    public void applyChange(Deployment deployment, UpdateDeploymentDto deploymentDto) {
        deployment
                .setDescription(
                        deploymentDto.getDescription()
                )
                .setStartDateTime(
                        Instant.ofEpochMilli(deploymentDto.getStartDateTime()).atZone(zoneId).toLocalDateTime()
                );
        if (deployment instanceof LaunchDeployment) {
            LaunchDeployment launchDeployment = (LaunchDeployment) deployment;
            launchDeployment.getWorkersToLaunch().clear();
            launchDeployment.getWorkersToLaunch().addAll(
                    deploymentDto.getWorkers()
                            .stream()
                            .map(ovhRegionWorkerMapper::convertToModel)
                            .map(ovhRegionWorker -> ovhRegionWorker.setLaunchDeployment(launchDeployment))
                            .collect(Collectors.toSet())
            );
            launchDeployment.getSessionsToLaunch().clear();
            launchDeployment.getSessionsToLaunch().addAll(
                    deploymentDto.getSessions()
                            .stream()
                            .map(courseSessionRepository::getReferenceById)
                            .map(courseSession -> courseSession.setLaunchDeployment(launchDeployment))
                            .collect(Collectors.toSet())
            );
        } else {
            CleanDeployment cleanDeployment = (CleanDeployment) deployment;
            cleanDeployment.getWorkersToClean().clear();
            cleanDeployment.getWorkersToClean().addAll(
                    deploymentDto.getWorkers()
                            .stream()
                            .map(OVHRegionWorkerDto::getId)
                            .map(ovhRegionWorkerRepository::getReferenceById)
                            .map(ovhRegionWorker -> ovhRegionWorker.setCleanDeployment(cleanDeployment))
                            .collect(Collectors.toSet())
            );
            cleanDeployment.getSessionsToClean().clear();
            cleanDeployment.getSessionsToClean().addAll(
                    deploymentDto.getSessions()
                            .stream()
                            .map(courseSessionRepository::getReferenceById)
                            .map(courseSession -> courseSession.setCleanDeployment(cleanDeployment))
                            .collect(Collectors.toSet())
            );
        }
    }
}
