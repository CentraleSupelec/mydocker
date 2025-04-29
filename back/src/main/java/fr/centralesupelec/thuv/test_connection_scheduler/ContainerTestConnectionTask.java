package fr.centralesupelec.thuv.test_connection_scheduler;

import fr.centralesupelec.gRPC.ContainerStatusRequest;
import fr.centralesupelec.thuv.model.ConnectionType;
import fr.centralesupelec.thuv.service.ContainerStatusConfigureService;
import fr.centralesupelec.thuv.storage.ContainerStorage;
import fr.centralesupelec.thuv.dtos.ContainerPortDto;
import fr.centralesupelec.thuv.dtos.ContainerStatusDto;
import fr.centralesupelec.thuv.test_connection_scheduler.dtos.ContainerScheduledDto;
import org.apache.commons.lang3.StringUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Objects;

@RequiredArgsConstructor
public class ContainerTestConnectionTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ContainerTestConnectionTask.class);
    private final ContainerStorage containerStorage;
    private final ContainerTestConnectionTaskScheduler containerTestConnectionTaskScheduler;
    private final ContainerScheduledDto containerScheduledDto;
    private final ContainerTestParameterConfiguration containerTestParameterConfiguration;
    private final NodeIPRequestService nodeIPRequestService;
    private final TestSocket testSocket;
    private final ContainerStatusConfigureService containerStatusConfigureService;
    @Override

    public void run() {
        if (containerScheduledDto.getContainerDto().getStatus() == ContainerStatusDto.KO) {
            return;
        }
        // For "no suitable node", we want to wait for an available node to pop.
        // If it never pops, the TestConnectionTask will mark the container status as KO.
        if (
                StringUtils.isNotBlank(containerScheduledDto.getContainerDto().getCreationError())
                        && !containerScheduledDto.getContainerDto().getCreationError().contains("no suitable node")
        ) {
            containerScheduledDto.getContainerDto().setStatus(ContainerStatusDto.KO);
            containerStorage.addContainer(
                    containerScheduledDto.getContainerDto(),
                    containerScheduledDto.getUserId(),
                    containerScheduledDto.getCourseId()
            );
            containerStatusConfigureService.configureContainerStatus(
                    containerScheduledDto.getCourseId(),
                    containerScheduledDto.getUserId(),
                    ContainerStatusRequest.Action.off
            );
            return;
        }
        boolean succededConnection = testAllConnection();
        Long maxConnectionRetry = containerScheduledDto.getContainerDto().getNeedsNewGpu()
                ? containerTestParameterConfiguration.getNumberOfConnectionRetryForGpu()
                : containerTestParameterConfiguration.getNumberOfConnectionRetry();

        if (succededConnection) {
            String nodeIP = nodeIPRequestService.getNodeIP(
                    containerScheduledDto.getUserId(), containerScheduledDto.getCourseId()
            );
            if (!StringUtils.isBlank(nodeIP)) {
                containerScheduledDto.getContainerDto().setIp(nodeIP);
            }
            containerScheduledDto.getContainerDto().setStatus(ContainerStatusDto.OK);
            containerStorage.addContainer(
                    containerScheduledDto.getContainerDto(),
                    containerScheduledDto.getUserId(),
                    containerScheduledDto.getCourseId()
            );
            containerStatusConfigureService.configureContainerStatus(
                    containerScheduledDto.getCourseId(),
                    containerScheduledDto.getUserId(),
                    ContainerStatusRequest.Action.off
            );
        } else {
            if (containerScheduledDto.getNumberOfRetry() > maxConnectionRetry) {
                logger.error("Could not connect to container with user id: '" + containerScheduledDto.getUserId()
                        + "' and courseId: '" + containerScheduledDto.getCourseId() + "' after "
                        + maxConnectionRetry + " retry");
                containerScheduledDto.getContainerDto().setStatus(ContainerStatusDto.KO);
                containerStorage.addContainer(
                        containerScheduledDto.getContainerDto(),
                        containerScheduledDto.getUserId(),
                        containerScheduledDto.getCourseId()
                );
                containerStatusConfigureService.configureContainerStatus(
                        containerScheduledDto.getCourseId(),
                        containerScheduledDto.getUserId(),
                        ContainerStatusRequest.Action.off
                );
            } else {
                logger.debug(
                        "Reschedule connection try for container with user id: '" + containerScheduledDto.getUserId()
                        + "' and courseId: '" + containerScheduledDto.getCourseId() + "' after "
                        + containerScheduledDto.getNumberOfRetry() + "/"
                        + maxConnectionRetry + " retries"
                );
                containerScheduledDto
                        .setNumberOfRetry(
                                containerScheduledDto.getNumberOfRetry() + 1
                        )
                        .setLastExecution(
                                LocalDateTime.now()
                        );
                containerTestConnectionTaskScheduler.addContainerScheduledDto(containerScheduledDto);
            }
        }
    }

    private boolean testAllConnection() {
        return containerScheduledDto.getContainerDto().getPorts()
                .stream()
                .allMatch(this::testPortConnection);
    }

    private boolean testPortConnection(ContainerPortDto p) {
        if (!p.getRequiredToAccessContainer()) {
            return true;
        }
        if (
                (Objects.equals(p.getConnectionType(), ConnectionType.HTTP.name()))
                        && !StringUtils.isBlank(p.getHostname())
        ) {
            return testSocket.isHttpsAlive(
                    p.getHostname(),
                    containerTestParameterConfiguration.getTimeInSecondBeforeConnectionTestTimeout()
            );
        }
        return TestSocket.isSocketAlive(
                containerScheduledDto.getContainerDto().getIp(),
                p.getPortMapTo(),
                containerTestParameterConfiguration.getTimeInSecondBeforeConnectionTestTimeout()
        );
    }
}
