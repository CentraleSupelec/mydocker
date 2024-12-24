package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.ContainerStatusRequest;
import fr.centralesupelec.gRPC.ContainerStatusResponse;
import fr.centralesupelec.thuv.dtos.ContainerDto;
import fr.centralesupelec.thuv.dtos.ContainerStatusDto;
import fr.centralesupelec.thuv.dtos.ContainerSwarmStateDto;
import fr.centralesupelec.thuv.storage.ContainerStorage;
import fr.centralesupelec.thuv.test_connection_scheduler.ContainerTestConnectionTaskScheduler;
import fr.centralesupelec.thuv.test_connection_scheduler.dtos.ContainerScheduledDto;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ContainerStatusResponseStreamObserver implements StreamObserver<ContainerStatusResponse> {
    private final Logger logger = LoggerFactory.getLogger(ContainerStatusResponseStreamObserver.class);
    private final ContainerStorage containerStorage;
    // Not final to avoid circular dependency
    private ContainerStatusConfigureService containerStatusConfigureService;
    private final ContainerTestConnectionTaskScheduler containerTestConnectionTaskScheduler;

    public void onNext(ContainerStatusResponse containerStatusResponse) {
        ContainerDto containerDto = containerStorage.getContainer(
                containerStatusResponse.getUserID(), containerStatusResponse.getCourseID()
        ).orElse(null);
        logger.debug(String.format(
                "ContainerStatusResponse received for courseId %s / userId %s / admin %s ; containerDto is %s",
                containerStatusResponse.getCourseID(),
                containerStatusResponse.getUserID(),
                containerStatusResponse.getIsAdmin(),
                containerDto
        ));
        if (containerDto == null) {
            return;
        }
        switch (containerStatusResponse.getState()) {
            case FAILED, REJECTED, ORPHANED, SHUTDOWN -> {
                containerDto.setStatus(ContainerStatusDto.KO);
                containerStatusConfigureService.configureContainerStatus(
                        containerStatusResponse.getCourseID(),
                        containerStatusResponse.getUserID(),
                        ContainerStatusRequest.Action.off
                );
            }
            case RUNNING -> {
                if (containerDto.getStatus() != ContainerStatusDto.CHECKING) {
                    containerDto.setStatus(ContainerStatusDto.CHECKING);
                    containerTestConnectionTaskScheduler.addContainerScheduledDto(
                            new ContainerScheduledDto()
                                    .setContainerDto(containerDto)
                                    .setUserId(containerStatusResponse.getUserID())
                                    .setCourseId(containerStatusResponse.getCourseID())
                    );
                }
            }
            case PENDING -> {
                if (containerStatusResponse.getErrorMessage().contains("no suitable node")) {
                    containerTestConnectionTaskScheduler.addContainerScheduledDto(
                            new ContainerScheduledDto()
                                    .setContainerDto(containerDto)
                                    .setUserId(containerStatusResponse.getUserID())
                                    .setCourseId(containerStatusResponse.getCourseID())
                    );
                }
            }
            default -> {
                containerDto.setStatus(ContainerStatusDto.PENDING);
            }
        }
        containerStorage.setContainerState(
                containerStatusResponse.getUserID(),
                containerStatusResponse.getCourseID(),
                ContainerSwarmStateDto.valueOf(containerStatusResponse.getState().name())
        );
        if (!StringUtils.isBlank(containerStatusResponse.getErrorMessage())) {
            containerDto.setCreationError(containerStatusResponse.getErrorMessage());
        }
    }

    public void onError(Throwable throwable) {
        logger.error("Error requesting a container status.\n Please restart the go API then this service.");
        if (logger.isDebugEnabled()) {
            throwable.printStackTrace();
        }
    }

    public void onCompleted() {
    }

    public void setContainerStatusConfigureService(ContainerStatusConfigureService containerStatusConfigureService) {
        this.containerStatusConfigureService = containerStatusConfigureService;
        this.containerTestConnectionTaskScheduler.setContainerStatusConfigureService(containerStatusConfigureService);
    }

}
