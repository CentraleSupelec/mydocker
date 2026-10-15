package fr.centralesupelec.thuv.scale_up.mappers;

import fr.centralesupelec.thuv.mappers.SessionWithCourseMapper;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.model.ComputeType;
import fr.centralesupelec.thuv.permissions.exceptions.ClusterNotFoundException;
import fr.centralesupelec.thuv.repository.ClusterRepository;
import fr.centralesupelec.thuv.repository.ComputeTypeRepository;
import fr.centralesupelec.thuv.repository.CourseSessionRepository;
import fr.centralesupelec.thuv.scale_up.dtos.*;
import fr.centralesupelec.thuv.scale_up.model.CleanDeployment;
import fr.centralesupelec.thuv.scale_up.model.Deployment;
import fr.centralesupelec.thuv.scale_up.model.LaunchDeployment;
import fr.centralesupelec.thuv.scale_up.model.OVHRegionWorker;
import fr.centralesupelec.thuv.scale_up.repository.OVHRegionWorkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
    private final ClusterRepository clusterRepository;
    private final ComputeTypeRepository computeTypeRepository;

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

        if (deployment.getCluster() instanceof Cluster) {
                deploymentDto.setCluster(deployment.getCluster().getId());
        }

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

    public void applyChange(Deployment deployment, UpdateDeploymentDto deploymentDto) throws ClusterNotFoundException {
        deploymentDto.getWorkers().forEach(
                worker -> {
                        // A worker with no compute type belongs to the deployment's cluster.
                        if (worker.getComputeTypeId() == null) {
                                return;
                        }
                        rejectOtherCluster(
                                computeTypeRepository.getReferenceById(worker.getComputeTypeId()),
                                deploymentDto.getCluster(),
                                String.format(
                                        "Compute type %d is linked to a different cluster than that of the deployment",
                                        worker.getComputeTypeId()
                                )
                        );
                }
        );
        deploymentDto.getSessions().forEach(
                sessionId -> rejectOtherCluster(
                        courseSessionRepository.getReferenceById(sessionId).getCourse().getComputeType(),
                        deploymentDto.getCluster(),
                        String.format("Session %d belongs to a course on a different cluster than that of the deployment", sessionId)
                )
        );
        if (deployment instanceof CleanDeployment) {
            deploymentDto.getWorkers().forEach(worker -> {
                OVHRegionWorker stored = ovhRegionWorkerRepository.getReferenceById(worker.getId());
                String message = String.format(
                        "Worker %d runs on a different cluster than that of the deployment", worker.getId()
                );
                if (stored.getComputeType() != null) {
                    rejectOtherCluster(stored.getComputeType(), deploymentDto.getCluster(), message);
                    return;
                }
                // No compute type: the worker runs on the cluster of the deployment that launched it.
                Cluster launchedOn = stored.getLaunchDeployment() == null ? null : stored.getLaunchDeployment().getCluster();
                if (launchedOn != null && !launchedOn.getId().equals(deploymentDto.getCluster())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
                }
            });
        }
        Cluster cluster = clusterRepository.getReferenceById(deploymentDto.getCluster());
        deployment
                .setDescription(
                        deploymentDto.getDescription()
                )
                .setStartDateTime(
                        Instant.ofEpochMilli(deploymentDto.getStartDateTime()).atZone(zoneId).toLocalDateTime()
                )
                .setCluster(cluster)
                ;
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

    // Compared by id: entities are Lombok @Data, so equals on a lazy proxy compares fields.
    // A compute type without a cluster is not rejected here; assigning those is the upgrade data migration.
    private static void rejectOtherCluster(ComputeType computeType, Long clusterId, String message) {
        if (computeType == null || computeType.getCluster() == null) {
            return;
        }
        if (!computeType.getCluster().getId().equals(clusterId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }
}
