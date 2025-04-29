package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.ContainerResponse;
import fr.centralesupelec.gRPC.ContainerStatusRequest;
import fr.centralesupelec.thuv.dtos.ContainerDto;
import fr.centralesupelec.thuv.mappers.GrpcResponsePortToContainerPortDtoMapper;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.repository.CourseRepository;
import fr.centralesupelec.thuv.repository.UserCourseRepository;
import fr.centralesupelec.thuv.storage.ContainerStorage;
import io.grpc.stub.StreamObserver;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContainerResponseStreamObserver implements StreamObserver<ContainerResponse> {
    private static final Logger logger = LoggerFactory.getLogger(ContainerResponseStreamObserver.class);

    private final GrpcResponsePortToContainerPortDtoMapper grpcResponsePortToContainerPortDtoMapper;
    private final UserCourseRepository userCourseRepository;
    private final CourseRepository courseRepository;
    private final ContainerStatusConfigureService containerStatusConfigureService;
    private final ContainerStorage containerStorage;

    @Override
    public void onNext(ContainerResponse containerResponse) {
        ContainerDto containerDto = mapContainerResponseToContainer(containerResponse);
        saveUserPasswordInDb(containerResponse);
        logger.debug(
                "ContainerResponse received for courseId {} / userId {} ; containerDto is {}",
                containerResponse.getCourseID(),
                containerResponse.getUserID(),
                containerDto
        );
        containerStorage.addContainer(containerDto, containerResponse.getUserID(), containerResponse.getCourseID());
        containerStatusConfigureService.configureContainerStatus(
                containerResponse.getCourseID(),
                containerResponse.getUserID(),
                ContainerStatusRequest.Action.on
        );
    }

    @Override
    public void onError(Throwable throwable) {
        logger.error("Error requesting a container.\n Please restart the go API then this service.");
        if (logger.isDebugEnabled()) {
            throwable.printStackTrace();
        }
        Sentry.captureMessage("Unable to obtain the container");
    }

    @Override
    public void onCompleted() {
    }

    private ContainerDto mapContainerResponseToContainer(ContainerResponse containerResponse) {
        ContainerDto containerDto = new ContainerDto();
        Optional<Course> course;
        try {
            course = courseRepository.findById(Long.parseLong(containerResponse.getCourseID()));
        } catch (NumberFormatException ignored) {
            course = Optional.empty();
        }

        containerDto.setIp(containerResponse.getIpAddress());
        containerDto.setPorts(
                containerResponse.getPortsList()
                    .stream()
                    .map(grpcResponsePortToContainerPortDtoMapper::convertToContainerPortDto)
                    .collect(Collectors.toList())
        );

        // Need to change it if different auth method
        containerDto.setPassword(containerResponse.getUserPassword().getPassword());
        containerDto.setUsername(containerResponse.getUserPassword().getUsername());
        containerDto.setCreationError(containerResponse.getError());
        containerDto.getErrorParams().putAll(containerResponse.getErrorParamsMap());
        containerDto.setDeletionTime(containerResponse.getDeletionTime());
        containerDto.setNeedsNewGpu(course.isPresent() && course.get().getComputeType().isGpu());
        return containerDto;
    }

    private void saveUserPasswordInDb(ContainerResponse containerResponse) {
        try {
            userCourseRepository.findByUserIdAndCourseId(
                    Long.parseLong(containerResponse.getUserID()),
                    Long.parseLong(containerResponse.getCourseID())
            ).ifPresent(
                    userCourse -> {
                        userCourse
                                .setUsername(
                                        containerResponse.getUserPassword().getUsername()
                                )
                                .setPassword(
                                        containerResponse.getUserPassword().getPassword()
                                );
                        userCourseRepository.saveAndFlush(userCourse);
                    }
            );
        } catch (NumberFormatException ignored) {
            return;
        }
    }
}
