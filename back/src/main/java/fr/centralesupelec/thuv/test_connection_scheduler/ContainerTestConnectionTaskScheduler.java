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
import org.springframework.core.task.TaskRejectedException;
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
    // Keys whose task has been dispatched but has not finished yet. They are no longer in the map
    // above, and without this set nothing dedupes an insert made while the test is running.
    private final Set<String> inFlightKeys = ConcurrentHashMap.newKeySet();
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
                )
                .filter(
                        c -> !inFlightKeys.contains(
                                ContainerUtilsService.generateKey(c.getUserId(), c.getCourseId())
                        )
                ).collect(Collectors.toSet());

        toTest
                .forEach(
                        c -> {
                            logger.debug("Current dto: " + c);
                            String key = ContainerUtilsService.generateKey(c.getUserId(), c.getCourseId());
                            ContainerTestConnectionTask task = new ContainerTestConnectionTask(
                                    containerStorage,
                                    this,
                                    c,
                                    containerTestParameterConfiguration,
                                    nodeIPRequestService,
                                    testSocket,
                                    containerStatusConfigureService
                            );
                            // Claimed and taken out of the map one dto at a time, immediately
                            // before its own submission, so a rejection leaves the dtos still
                            // waiting behind it untouched.
                            inFlightKeys.add(key);
                            containerScheduledDtos.remove(key);
                            try {
                                applicationTaskExecutor
                                        .execute(
                                                () -> {
                                                    try {
                                                        task.run();
                                                    } finally {
                                                        // Released only once the task has put the dto
                                                        // back for a retry, so the key is never both
                                                        // unclaimed and unscheduled.
                                                        inFlightKeys.remove(key);
                                                    }
                                                }
                                        );
                            } catch (TaskRejectedException e) {
                                // A full pool is the one failure worth recovering from here, and
                                // only that one: anything else thrown by the executor is a fault
                                // that has to surface. Nothing will run and nothing will release
                                // the claim, so the dto goes back in the map before the key is
                                // freed. Its lastExecution is unchanged, so the next tick submits
                                // it again, and the other dtos of this round keep their turn.
                                logger.warn(
                                        "Could not submit the connection test for {}, rescheduling it: {}",
                                        key,
                                        e.getMessage()
                                );
                                containerScheduledDtos.put(key, c);
                                inFlightKeys.remove(key);
                            } catch (RuntimeException e) {
                                // The fault surfaces, but not with the environment left out of the
                                // map behind a claim no runnable exists to release, which would
                                // bar it from every later round.
                                containerScheduledDtos.put(key, c);
                                inFlightKeys.remove(key);
                                throw e;
                            }
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
        return containerScheduledDtos.containsKey(key) || inFlightKeys.contains(key);
    }
}
