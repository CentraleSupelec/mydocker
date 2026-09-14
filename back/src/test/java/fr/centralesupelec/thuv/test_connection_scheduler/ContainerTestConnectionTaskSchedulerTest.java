package fr.centralesupelec.thuv.test_connection_scheduler;

import fr.centralesupelec.thuv.activity_logging.services.ActivityLogger;
import fr.centralesupelec.thuv.dtos.ContainerDto;
import fr.centralesupelec.thuv.dtos.ContainerStatusDto;
import fr.centralesupelec.thuv.service.ContainerUtilsService;
import fr.centralesupelec.thuv.storage.ContainerStorage;
import fr.centralesupelec.thuv.test_connection_scheduler.dtos.ContainerScheduledDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class ContainerTestConnectionTaskSchedulerTest {
    private static final String USER_ID = "user1";
    private static final String COURSE_ID = "course1";

    @Mock
    private ActivityLogger activityLogger;

    @Mock
    private ContainerTestParameterConfiguration configuration;

    @Mock
    private NodeIPRequestService nodeIPRequestService;

    @Mock
    private TestSocket testSocket;

    private final List<Runnable> submittedTasks = new ArrayList<>();
    private int submissionsToReject = 0;
    private int submissionsToFailUnexpectedly = 0;
    private final TaskExecutor taskExecutor = runnable -> {
        if (submissionsToReject > 0) {
            submissionsToReject--;
            throw new TaskRejectedException("no thread available");
        }
        if (submissionsToFailUnexpectedly > 0) {
            submissionsToFailUnexpectedly--;
            throw new IllegalStateException("executor is broken");
        }
        submittedTasks.add(runnable);
    };

    private ContainerTestConnectionTaskScheduler scheduler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        submittedTasks.clear();
        scheduler = new ContainerTestConnectionTaskScheduler(
                new ContainerStorage(activityLogger),
                taskExecutor,
                configuration,
                nodeIPRequestService,
                testSocket
        );
    }

    /**
     * The dto leaves the map before the task is dispatched. Until the key is claimed for the whole
     * run, a status response arriving in that window found nothing scheduled and queued a second
     * task for the same environment, with a retry counter starting again at zero.
     */
    @Test
    void anEnvironmentStaysScheduledUntilItsTaskHasFinished() {
        when(configuration.getTimeInSecondBetweenTwoConnectionsTry()).thenReturn(5L);
        scheduler.addContainerScheduledDto(scheduledDto(USER_ID, COURSE_ID));
        String key = ContainerUtilsService.generateKey(USER_ID, COURSE_ID);

        scheduler.scheduleConnectionTest();

        assertEquals(1, submittedTasks.size());
        assertTrue(
                scheduler.containerScheduledDtoExists(key),
                "the key must stay claimed while the task is queued or running"
        );

        submittedTasks.get(0).run();

        assertFalse(scheduler.containerScheduledDtoExists(key));
    }

    /**
     * A pool that has no thread left rejects the submission. Nothing will run, so nothing will
     * release the claim either: the environment has to go back in the schedule, and the ones
     * queued behind it in the same round have to keep their turn.
     */
    @Test
    void aRejectedSubmissionLeavesTheEnvironmentScheduledForTheNextRound() {
        when(configuration.getTimeInSecondBetweenTwoConnectionsTry()).thenReturn(5L);
        scheduler.addContainerScheduledDto(scheduledDto(USER_ID, COURSE_ID));
        String key = ContainerUtilsService.generateKey(USER_ID, COURSE_ID);
        submissionsToReject = 1;

        scheduler.scheduleConnectionTest();

        assertEquals(0, submittedTasks.size());
        assertTrue(
                scheduler.containerScheduledDtoExists(key),
                "a rejected environment must stay scheduled instead of being dropped"
        );

        scheduler.scheduleConnectionTest();

        assertEquals(1, submittedTasks.size(), "the key must not stay claimed after a rejection");
    }

    @Test
    void aRejectedSubmissionDoesNotStrandTheRestOfTheRound() {
        when(configuration.getTimeInSecondBetweenTwoConnectionsTry()).thenReturn(5L);
        scheduler.addContainerScheduledDto(scheduledDto(USER_ID, COURSE_ID));
        scheduler.addContainerScheduledDto(scheduledDto("user2", "course2"));
        submissionsToReject = 1;

        scheduler.scheduleConnectionTest();

        assertEquals(1, submittedTasks.size(), "the environment behind the rejected one must still be submitted");

        // The submitted task finishes and releases its own key; the rejected one is still due.
        submittedTasks.get(0).run();
        scheduler.scheduleConnectionTest();

        assertEquals(2, submittedTasks.size());
    }

    /**
     * An executor failure that is not a rejection is a fault and must surface, but it must not
     * leave the environment out of the map behind a claim that no runnable exists to release.
     */
    @Test
    void anUnexpectedExecutorFailureSurfacesWithoutStrandingTheEnvironment() {
        when(configuration.getTimeInSecondBetweenTwoConnectionsTry()).thenReturn(5L);
        scheduler.addContainerScheduledDto(scheduledDto(USER_ID, COURSE_ID));
        String key = ContainerUtilsService.generateKey(USER_ID, COURSE_ID);
        submissionsToFailUnexpectedly = 1;

        assertThrows(IllegalStateException.class, () -> scheduler.scheduleConnectionTest());

        assertTrue(scheduler.containerScheduledDtoExists(key));

        scheduler.scheduleConnectionTest();

        assertEquals(1, submittedTasks.size(), "the environment must be submittable in a later round");
    }

    private ContainerScheduledDto scheduledDto(String userId, String courseId) {
        ContainerDto containerDto = new ContainerDto(
                "user-1",
                "password",
                new ArrayList<>(),
                LocalDateTime.now()
        );
        containerDto.setStatus(ContainerStatusDto.CHECKING);
        containerDto.setNeedsNewGpu(false);
        return new ContainerScheduledDto()
                .setContainerDto(containerDto)
                .setUserId(userId)
                .setCourseId(courseId);
    }
}
