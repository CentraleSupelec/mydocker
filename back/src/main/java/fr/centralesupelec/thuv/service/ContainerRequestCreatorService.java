package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.*;
import fr.centralesupelec.thuv.mappers.PortToGrpcRequestPortMapper;
import fr.centralesupelec.thuv.model.ComputeType;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.CourseSession;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.repository.ComputeTypeRepository;
import fr.centralesupelec.thuv.repository.UserCourseRepository;
import fr.centralesupelec.thuv.scale_up.model.Deployment;
import fr.centralesupelec.thuv.scale_up.model.LaunchDeployment;
import fr.centralesupelec.thuv.scale_up.model.OVHRegionWorker;
import fr.centralesupelec.thuv.scale_up.repository.DeploymentRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class ContainerRequestCreatorService {
        private static final Logger logger = LoggerFactory.getLogger(ContainerRequestCreatorService.class);
        private final DeploymentRepository deploymentRepository;
        private final PortToGrpcRequestPortMapper portToGrpcRequestPortMapper;
        private final UserCourseRepository userCourseRepository;
        private final ContainerUtilsService containerUtilsService;
        private final ComputeTypeRepository computeTypeRepository;

    public ContainerRequest createRequest(CourseSession courseSession, User user, boolean forceRecreate) {
        Course course = courseSession.getCourse();

        ContainerRequestOptions.Builder builder = ContainerRequestOptions.newBuilder()
                .setForceRecreate(forceRecreate)
                .setSaveStudentWork(course.isSaveStudentWork())
                .setCommand(course.getCommand())
        ;

        if (course.getComputeType().isGpu()) {
            String gpuResource = "gpu";
            builder.putGenericResources(gpuResource, 1);
        }

        String ownerKeyLabel = "owner";
        if (!course.getComputeType().getTechnicalName().isEmpty()) {
            builder.addMandatoryLabels(
                    Label.newBuilder()
                            .setKey(ownerKeyLabel)
                            .setValue(course.getComputeType().getTechnicalName())
                            .build()
            );
        } else {
            List<ComputeType> computeTypes = this.computeTypeRepository.findAll();
            builder.addAllForbiddenLabels(
                    computeTypes
                            .stream()
                            .filter(computeType -> !computeType.getTechnicalName().isEmpty())
                            .map(
                                    computeType ->
                                            Label
                                                    .newBuilder()
                                                    .setKey(ownerKeyLabel)
                                                    .setValue(computeType.getTechnicalName())
                                                    .build()
                            )
                            .collect(Collectors.toList())
            )
            ;
        }
        LocalDateTime now = LocalDateTime.now();

        // Get deplyments that have been successul
        List<Deployment> deployments = deploymentRepository.findByStartDateTimeBeforeAndStatusOrderByStartDateTimeDesc(
                now, Deployment.Status.OK
        );

        // Get launch deplyments that are linked to the course of the session
        List<LaunchDeployment> launchDeployments = deployments.stream()
                .filter(deployment -> deployment instanceof LaunchDeployment)
                .map(deployment -> (LaunchDeployment) deployment)
                .filter(launchDeployment -> launchDeployment.getSessionsToLaunch().stream()
                        .anyMatch(session -> 
                                session.getCourse().getId().equals(courseSession.getCourse().getId())
                        )
                )
                .toList();
        
        Long launchedCount = launchDeployments.stream()
                .flatMap(launchDeployment -> launchDeployment.getWorkersToLaunch().stream())
                .mapToLong(OVHRegionWorker::getCount)
                .sum();
        
        Long cleanedCount = launchDeployments.stream()
                .flatMap(launchDeployment -> launchDeployment.getSessionsToLaunch().stream())
                .filter(session -> session.getCourse().getId().equals(courseSession.getCourse().getId()))
                .map(CourseSession::getCleanDeployment)
                .filter(Objects::nonNull)
                .filter(cleanDeployment -> cleanDeployment.getStatus().equals(Deployment.Status.OK))
                .flatMap(cleanDeployment -> cleanDeployment.getWorkersToClean().stream())
                .mapToLong(OVHRegionWorker::getCount)
                .sum();

        if (launchedCount - cleanedCount > 0) {
                builder.addMandatoryLabels(
                        Label.newBuilder()
                            .setKey(String.format("courseId-%s", courseSession.getCourse().getId()))
                            .setValue("true")
                            .build()
                );
        } else {
                builder.addMandatoryLabels(
                        Label.newBuilder()
                            .setKey("shared-pool")
                            .setValue("true")
                            .build()
                );
        }

        if (useDedicatedWorker(courseSession.getCourse().getId())) {
                builder.addMandatoryLabels(
                        Label.newBuilder()
                            .setKey(String.format("courseId-%s", courseSession.getCourse().getId()))
                            .setValue("true")
                            .build()
                );
        } else {
                builder.addMandatoryLabels(
                        Label.newBuilder()
                            .setKey("shared-pool")
                            .setValue("true")
                            .build()
                );
        }

        if (course.isSaveStudentWork()) {
            builder.setWorkdirPath(course.getWorkdirPath())
                    .setWorkdirSize(course.getWorkdirSize());
        }
        if (course.getUseStudentVolume()) {
            builder
                    .setUseStudentVolume(true)
                    .setStudentVolumePath(course.getStudentVolumePath());
        }

        userCourseRepository.findByUserIdAndCourseId(
                user.getId(), course.getId()
        ).ifPresent(
            userCourse -> {
                if (StringUtils.isEmpty(userCourse.getUsername()) || StringUtils.isEmpty(userCourse.getPassword())) {
                    return;
                }
                builder.setUserPassword(
                        UserPasswordMethod.newBuilder()
                                .setUsername(
                                        userCourse.getUsername()
                                )
                                .setPassword(
                                        userCourse.getPassword()
                                )
                                .build()
                );
            }
        );

        if (course.getNanoCpusLimit() != null || course.getMemoryBytesLimit() != null) {
            builder.setLimit(
                    ResourceLimit.newBuilder()
                            .setMemoryBytes(
                                    course.getMemoryBytesLimit() != null ? course.getMemoryBytesLimit() : 0
                            )
                            .setNanoCPUs(
                                    course.getNanoCpusLimit() != null ? course.getNanoCpusLimit() : 0
                            )
                            .build()
            );
        }

        builder.setStorageBackend(StorageBackend.valueOf(course.getComputeType().getStorageBackend().toString()));

        Metadata.Builder metadataBuilder = Metadata.newBuilder()
                .putTags("courseId", String.valueOf(course.getId()))
                .putTags("courseTitle", course.getTitle())
                .putTags("userId", String.valueOf(user.getId()))
                .putTags("email", String.valueOf(user.getUsername()));

        Long deletionTime = containerUtilsService.computeDeletionTime(courseSession);
        if (deletionTime != null) {
            metadataBuilder.putTags("deleteAfter", "true");
            metadataBuilder.putTags("deletionTime", String.valueOf(deletionTime));
        }

        ContainerRequest.Builder containerRequestBuilder = ContainerRequest.newBuilder()
                .setUserID(
                        String.valueOf(user.getId())
                )
                .setImageID(
                        course.getDockerImage()
                )
                .setCourseID(
                        String.valueOf(course.getId())
                )
                .setMetadata(
                        metadataBuilder.build()
                )
                .setOptions(
                        builder.build()
                )
                .addAllPorts(
                        course.getPorts()
                                .stream()
                                .map(portToGrpcRequestPortMapper::mapToRequestPort)
                                .collect(Collectors.toList())
                );
        return containerRequestBuilder.build();
    }

    private boolean useDedicatedWorker(Long courseId) {
        LocalDateTime now = LocalDateTime.now();

        // Get deployments that have been successul
        List<Deployment> deployments = deploymentRepository.findByStartDateTimeBeforeAndStatusOrderByStartDateTimeDesc(
                now, Deployment.Status.OK
        );

        // Get launch deployments that are linked to the course of the session
        List<LaunchDeployment> launchDeployments = deployments.stream()
                .filter(deployment -> deployment instanceof LaunchDeployment)
                .map(deployment -> (LaunchDeployment) deployment)
                .filter(launchDeployment -> launchDeployment.getSessionsToLaunch().stream()
                        .anyMatch(session -> 
                                session.getCourse().getId().equals(courseId)
                        )
                )
                .toList();
        
        Long launchedCount = launchDeployments.stream()
                .flatMap(launchDeployment -> launchDeployment.getWorkersToLaunch().stream())
                .mapToLong(OVHRegionWorker::getCount)
                .sum();
        
        Long cleanedCount = launchDeployments.stream()
                .flatMap(launchDeployment -> launchDeployment.getSessionsToLaunch().stream())
                .filter(session -> session.getCourse().getId().equals(courseId))
                .map(CourseSession::getCleanDeployment)
                .filter(Objects::nonNull)
                .filter(cleanDeployment -> cleanDeployment.getStatus().equals(Deployment.Status.OK))
                .flatMap(cleanDeployment -> cleanDeployment.getWorkersToClean().stream())
                .mapToLong(OVHRegionWorker::getCount)
                .sum();
        return launchedCount - cleanedCount > 0;
    }
}
