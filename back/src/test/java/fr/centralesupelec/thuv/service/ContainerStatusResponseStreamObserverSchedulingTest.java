package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.ContainerStatusResponse;
import fr.centralesupelec.thuv.activity_logging.model.LogAction;
import fr.centralesupelec.thuv.activity_logging.model.LogModelName;
import fr.centralesupelec.thuv.activity_logging.services.ActivityLogger;
import fr.centralesupelec.thuv.dtos.ContainerDto;
import fr.centralesupelec.thuv.dtos.ContainerStatusDto;
import fr.centralesupelec.thuv.storage.ContainerStorage;
import fr.centralesupelec.thuv.test_connection_scheduler.ContainerTestConnectionTaskScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The status stream ticks once a second for every watched environment. These tests hold the line
 * that a tick only starts a connection test for an environment still waiting for one: sending an
 * environment that is already ready round again wrote one activity row per second, per
 * environment, for as long as it was watched.
 */
class ContainerStatusResponseStreamObserverSchedulingTest {
    private static final String USER_ID = "1";
    private static final String COURSE_ID = "2";

    @Mock
    private ActivityLogger activityLogger;

    @Mock
    private ContainerTestConnectionTaskScheduler containerTestConnectionTaskScheduler;

    private ContainerStorage containerStorage;
    private ContainerStatusResponseStreamObserver observer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        containerStorage = new ContainerStorage(activityLogger);
        observer = new ContainerStatusResponseStreamObserver(
                containerStorage,
                containerTestConnectionTaskScheduler
        );
    }

    @Test
    void aSecondRunningTickOnAReadyEnvironmentSchedulesNothing() {
        ContainerDto containerDto = pendingContainerInStorage();

        observer.onNext(runningResponse());

        assertEquals(ContainerStatusDto.CHECKING, containerDto.getStatus());
        verify(containerTestConnectionTaskScheduler, times(1)).addContainerScheduledDto(any());

        // The connection test succeeds and puts the environment back in storage, which is the one
        // legitimate ENVIRONMENT_CREATED_OK row for this environment.
        containerDto.setStatus(ContainerStatusDto.OK);
        containerStorage.addContainer(containerDto, USER_ID, COURSE_ID);

        observer.onNext(runningResponse());

        assertEquals(ContainerStatusDto.OK, containerDto.getStatus());
        verify(containerTestConnectionTaskScheduler, times(1)).addContainerScheduledDto(any());
        verify(activityLogger, times(1)).log(
                LogAction.ENVIRONMENT_CREATED_OK,
                LogModelName.COURSE,
                COURSE_ID,
                USER_ID
        );
    }

    @Test
    void aRunningTickDoesNotQueueASecondTestWhileOneIsAlreadyScheduled() {
        ContainerDto containerDto = pendingContainerInStorage();
        when(containerTestConnectionTaskScheduler.containerScheduledDtoExists(anyString()))
                .thenReturn(true);

        observer.onNext(runningResponse());

        // A second scheduled dto would carry numberOfRetry = 0 and lastExecution = null, which
        // both restarts the retry cap and makes the scheduler run the test at once.
        verify(containerTestConnectionTaskScheduler, never()).addContainerScheduledDto(any());
        assertEquals(ContainerStatusDto.PENDING, containerDto.getStatus());
    }

    private ContainerDto pendingContainerInStorage() {
        ContainerDto containerDto = new ContainerDto(
                "user-1",
                "password",
                new ArrayList<>(),
                LocalDateTime.now()
        );
        containerDto.setNeedsNewGpu(false);
        containerStorage.addContainer(containerDto, USER_ID, COURSE_ID);
        return containerDto;
    }

    private static ContainerStatusResponse runningResponse() {
        return ContainerStatusResponse.newBuilder()
                .setUserID(USER_ID)
                .setCourseID(COURSE_ID)
                .setState(ContainerStatusResponse.State.RUNNING)
                .build();
    }
}
