package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.*;
import fr.centralesupelec.thuv.mappers.PortToGrpcRequestPortMapper;
import fr.centralesupelec.thuv.model.ComputeType;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.CourseSession;
import fr.centralesupelec.thuv.model.Path;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.repository.ComputeTypeRepository;
import fr.centralesupelec.thuv.repository.PathRepository;
import fr.centralesupelec.thuv.repository.UserCourseRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class ContainerRequestCreatorService {
        private static final Logger logger = LoggerFactory.getLogger(ContainerRequestCreatorService.class);
        private final PortToGrpcRequestPortMapper portToGrpcRequestPortMapper;
        private final UserCourseRepository userCourseRepository;
        private final ContainerUtilsService containerUtilsService;
        private final ComputeTypeRepository computeTypeRepository;
        private final CourseInfraRequestService courseInfraRequestService;
        private final PathRepository pathRepository;

    public ContainerRequest createRequest(CourseSession courseSession, User user, boolean forceRecreate) {
        Course course = courseSession.getCourse();
        return createRequest(
                course,
                user,
                forceRecreate,
                courseSession.getEndDateTime(),
                courseSession.getDestroyContainerAfterEndTime()
        );
    }

    public ContainerRequest createRequest(
        Course course, 
        User user,
        boolean forceRecreate,
        LocalDateTime sesssionEndDateTime,
        Boolean destroyContainerAfterEndTime
    ) {
        ContainerRequestOptions.Builder builder = ContainerRequestOptions.newBuilder()
                .setForceRecreate(forceRecreate)
                .setSaveStudentWork(course.isSaveStudentWork())
                .setCommand(course.getCommand())
        ;
        List<Path> paths = pathRepository.findAll();

        builder
                .addAllPaths(
                        paths
                                .stream()
                                .map(
                                        path ->
                                                fr.centralesupelec.gRPC.Path
                                                        .newBuilder()
                                                        .setValue(path.getValue())
                                                        .setRecursive(path.getRecursive())
                                                        .setChangeOwner(path.getChangeOwner())
                                                        .setPermissions(path.getPermissions())
                                                        .build()
                                )
                        .collect(Collectors.toList())
                );

        if (null != course.getUid()) {
                builder.setUid(course.getUid());
        }        

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
        if (courseInfraRequestService.getCourseInfra(Long.toString(course.getId()))) {
                logger.debug("Adding constraint to deploy on dedicated nodes for course '{}'", course.getId());
                builder.addMandatoryLabels(
                        Label.newBuilder()
                            .setKey(String.format("courseId-%s", course.getId()))
                            .setValue("true")
                            .build()
                );
        } else {
                logger.debug("Adding constraint to deploy on shared pool nodes for course '{}'", course.getId());
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

        Long deletionTime = null;
        if (sesssionEndDateTime != null) {
                deletionTime = containerUtilsService.computeDeletionTime(course, sesssionEndDateTime, destroyContainerAfterEndTime);
        }
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
}
