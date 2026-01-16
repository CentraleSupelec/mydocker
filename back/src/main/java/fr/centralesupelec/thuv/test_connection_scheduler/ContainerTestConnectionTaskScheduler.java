package fr.centralesupelec.thuv.test_connection_scheduler;

import fr.centralesupelec.thuv.dtos.ContainerStatusDto;
import fr.centralesupelec.thuv.service.ContainerStatusConfigureService;
import fr.centralesupelec.thuv.service.ContainerUtilsService;
import fr.centralesupelec.thuv.storage.ContainerStorage;
import fr.centralesupelec.thuv.test_connection_scheduler.dtos.ContainerScheduledDto;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ContainerTestConnectionTaskScheduler {
    private static final Logger logger = LoggerFactory.getLogger(ContainerTestConnectionTaskScheduler.class);
    private final ContainerStorage containerStorage;
    private final TaskExecutor applicationTaskExecutor;
    private final ContainerTestParameterConfiguration containerTestParameterConfiguration;
    private final NodeIPRequestService nodeIPRequestService;
    private final TestSocket testSocket;

    private final ConcurrentHashMap<String, ContainerScheduledDto> containerScheduledDtos = new ConcurrentHashMap<>();
    @Setter
    private ContainerStatusConfigureService containerStatusConfigureService;

    @Scheduled(fixedRate = 1000)
    @Profile("!test")
    public void scheduleConnectionTest() {
        LocalDateTime secondsAgo = LocalDateTime.now().minusSeconds(
                containerTestParameterConfiguration.getTimeInSecondBetweenTwoConnectionsTry()
        );
        logger.debug("ContainerScheduledDtos before executing : {}", containerScheduledDtos);
        containerScheduledDtos.values()
                .stream()
                .filter(
                        c -> c.getContainerDto().getStatus() == ContainerStatusDto.KO
                )
                .forEach(c -> containerScheduledDtos.remove(ContainerUtilsService.generateKey(c.getUserId(), c.getCourseId())));
        // Example cases : container started so was marked in "CHECKING" status and test was scheduled,
        // but entrypoint script failed

        Set<ContainerScheduledDto> toTest = containerScheduledDtos.values()
                .stream()
                .filter(
                        c -> c.getLastExecution() == null || c.getLastExecution().isBefore(secondsAgo)
                ).collect(Collectors.toSet());
        
        toTest.forEach(c -> containerScheduledDtos.remove(ContainerUtilsService.generateKey(c.getUserId(), c.getCourseId())));

        toTest
                .forEach(
                        c -> {
                            logger.debug("Current dto: " + c);
                            applicationTaskExecutor
                                    .execute(
                                            new ContainerTestConnectionTask(
                                                    containerStorage,
                                                    this,
                                                    c,
                                                    containerTestParameterConfiguration,
                                                    nodeIPRequestService,
                                                    testSocket,
                                                    containerStatusConfigureService
                                            )
                                    );
                        }
                );
        logger.debug("ContainerScheduledDtos after executing : {}", containerScheduledDtos);
    }

    public void addContainerScheduledDto(ContainerScheduledDto containerScheduledDto) {
        logger.debug("ContainerScheduledDtos before adding : {}", containerScheduledDtos);
        logger.debug("Adding dto: {}", containerScheduledDto);
        containerScheduledDtos.put(
                ContainerUtilsService.generateKey(containerScheduledDto.getUserId(), containerScheduledDto.getCourseId()),
                containerScheduledDto
        );
        logger.debug("ContainerScheduledDtos after adding : {}", containerScheduledDtos);
    }

    public boolean containerScheduledDtoExists(String key) {
        return containerScheduledDtos.containsKey(key);
    }
}
