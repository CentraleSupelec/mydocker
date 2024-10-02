package fr.centralesupelec.thuv.test_connection_scheduler;

import fr.centralesupelec.thuv.storage.ContainerStorage;
import fr.centralesupelec.thuv.test_connection_scheduler.dtos.ContainerScheduledDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ContainerTestConnectionTaskScheduler {
    private static final Logger logger = LoggerFactory.getLogger(ContainerTestConnectionTaskScheduler.class);
    private final ContainerStorage containerStorage;
    private final TaskExecutor taskExecutor;
    private final ContainerTestParameterConfiguration containerTestParameterConfiguration;
    private final NodeIPRequestService nodeIPRequestService;
    private final TestSocket testSocket;

    private final Set<ContainerScheduledDto> containerScheduledDtos = Collections.synchronizedSet(new HashSet<>());

    @Scheduled(fixedRate = 1000)
    @Profile("!test")
    public void scheduleConnectionTest() {
        LocalDateTime secondsAgo = LocalDateTime.now().minusSeconds(
                containerTestParameterConfiguration.getTimeInSecondBetweenTwoConnectionsTry()
        );
        logger.debug("ContainerScheduledDtos before executing : " + containerScheduledDtos);
        Set<ContainerScheduledDto> toTest = containerScheduledDtos
                .stream()
                .filter(
                        c -> c.getLastExecution() == null || c.getLastExecution().isBefore(secondsAgo)
                )
                .collect(Collectors.toSet());
        containerScheduledDtos.removeAll(toTest);
        toTest
                .forEach(
                        c -> {
                            logger.debug("Current dto: " + c);
                            taskExecutor
                                    .execute(
                                            new ContainerTestConnectionTask(
                                                    containerStorage,
                                                    this,
                                                    c,
                                                    containerTestParameterConfiguration,
                                                    nodeIPRequestService,
                                                    testSocket
                                            )
                                    );
                        }
                );
        logger.debug("ContainerScheduledDtos after executing : " + containerScheduledDtos);
    }

    public void addContainerScheduledDto(ContainerScheduledDto containerScheduledDto) {
        logger.debug("ContainerScheduledDtos before adding : " + containerScheduledDtos);
        logger.debug("Adding dto: " + containerScheduledDto);
        containerScheduledDtos.add(containerScheduledDto);
        logger.debug("ContainerScheduledDtos after adding : " + containerScheduledDtos);
    }
}
