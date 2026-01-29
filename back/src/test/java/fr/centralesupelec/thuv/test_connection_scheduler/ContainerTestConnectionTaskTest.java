package fr.centralesupelec.thuv.test_connection_scheduler;

import fr.centralesupelec.thuv.activity_logging.services.ActivityLogger;
import fr.centralesupelec.thuv.dtos.ContainerDto;
import fr.centralesupelec.thuv.dtos.ContainerPortDto;
import fr.centralesupelec.thuv.dtos.ContainerStatusDto;
import fr.centralesupelec.thuv.service.ContainerStatusConfigureService;
import fr.centralesupelec.thuv.storage.ContainerStorage;
import fr.centralesupelec.thuv.test_connection_scheduler.dtos.ContainerScheduledDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class ContainerTestConnectionTaskTest {

    private ContainerStorage containerStorage;

    @Mock
    private ActivityLogger activityLogger;

    @Mock
    private ContainerTestConnectionTaskScheduler scheduler;

    @Mock
    private ContainerTestParameterConfiguration configuration;

    @Mock
    private NodeIPRequestService nodeIPRequestService;

    @Mock
    private TestSocket testSocket;

    @Mock
    private ContainerStatusConfigureService containerStatusConfigureService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        containerStorage = new ContainerStorage(activityLogger);
    }

    @Test
    void regressionTestForRaceConditionWithDifferentCreatedAtDate() {
        String userId = "user1";
        String courseId = "course1";

        // 1. Create container C1 and add it to storage
        ContainerDto c1 = new ContainerDto("user-1", "password", new ArrayList<>(), LocalDateTime.now());
        c1.setIp("1.1.1.1");
        c1.setStatus(ContainerStatusDto.CHECKING);
        c1.setNeedsNewGpu(false);
        containerStorage.addContainer(c1, userId, courseId);

        // 2. Schedule task for C1
        ContainerScheduledDto cs1 = new ContainerScheduledDto()
                .setContainerDto(c1)
                .setUserId(userId)
                .setCourseId(courseId);

        // 3. Create container C2 and add it to storage (overwriting C1)
        ContainerDto c2 = new ContainerDto("user-1", "password", new ArrayList<>(), LocalDateTime.now());
        c2.setIp("2.2.2.2");
        c2.setStatus(ContainerStatusDto.CHECKING);
        c2.setNeedsNewGpu(false);
        containerStorage.addContainer(c2, userId, courseId);

        // Verify storage has C2
        assertEquals(c2, containerStorage.getContainer(userId, courseId).orElse(null));

        // 4. Run Task for C1
        // We mock some calls to make sure it reaches the end
        when(configuration.getNumberOfConnectionRetry()).thenReturn(5L);
        when(nodeIPRequestService.getNodeIP(anyString(), anyString())).thenReturn("1.1.1.1");

        ContainerTestConnectionTask task = new ContainerTestConnectionTask(
                containerStorage,
                scheduler,
                cs1,
                configuration,
                nodeIPRequestService,
                testSocket,
                containerStatusConfigureService
        );
        task.run();

        // 5. Assert that ContainerStorage contains C2
        // If the bug exists, storage will contain C1 because task.run() overwrote it.
        ContainerDto containerInStorage = containerStorage.getContainer(userId, courseId).get();
        assertEquals(c2.getIp(), containerInStorage.getIp());
        assertNotEquals(c1.getIp(), containerInStorage.getIp());
    }

    @Test
    void regressionTestForRaceConditionWithSameCreatedAtDate() {
        String userId = "user2";
        String courseId = "course2";
        LocalDateTime now = LocalDateTime.now();
        // 1. Create container C1 and add it to storage
        ContainerDto c1 = new ContainerDto("user-2", "password", new ArrayList<>(), now);
        c1.setIp("1.1.1.1");
        c1.setStatus(ContainerStatusDto.CHECKING);
        c1.setNeedsNewGpu(false);
        containerStorage.addContainer(c1, userId, courseId);

        // 2. Schedule task for C1
        ContainerScheduledDto cs1 = new ContainerScheduledDto()
                .setContainerDto(c1)
                .setUserId(userId)
                .setCourseId(courseId);

        // 3. Create container C2 and add it to storage (overwriting C1)
        ContainerDto c2 = new ContainerDto("user-2", "password", new ArrayList<>(), now);
        c2.setIp("2.2.2.2");
        c2.setStatus(ContainerStatusDto.CHECKING);
        c2.setNeedsNewGpu(false);
        containerStorage.addContainer(c2, userId, courseId);

        // Verify storage has C2
        assertEquals(c2.getIp(), containerStorage.getContainer(userId, courseId).get().getIp());
        assertNotEquals(c1.getIp(), containerStorage.getContainer(userId, courseId).get().getIp());

        // 4. Run Task for C1
        // We mock some calls to make sure it reaches the end
        when(configuration.getNumberOfConnectionRetry()).thenReturn(5L);
        when(nodeIPRequestService.getNodeIP(anyString(), anyString())).thenReturn("1.1.1.1");

        ContainerTestConnectionTask task = new ContainerTestConnectionTask(
                containerStorage,
                scheduler,
                cs1,
                configuration,
                nodeIPRequestService,
                testSocket,
                containerStatusConfigureService
        );
        task.run();

        // 5. Assert that ContainerStorage contains C1 because C1 and C2 are "equal" so C1 task overwrites C2
        ContainerDto containerInStorage = containerStorage.getContainer(userId, courseId).get();
        assertEquals(c1.getIp(), containerInStorage.getIp());
        assertNotEquals(c2.getIp(), containerInStorage.getIp());
    }


    @Test
    void regressionTestForRaceConditionWithDifferentPorts() {
        String userId = "user1";
        String courseId = "course1";
        LocalDateTime now = LocalDateTime.now();
        // 1. Create container C1 and add it to storage
        ContainerDto c1 = new ContainerDto("user-1", "password", List.of(
                (ContainerPortDto) new ContainerPortDto()
                        .setPortMapTo(8000)
                        .setConnectionType("HTTPS")
                        .setRequiredToAccessContainer(true)
                        .setHostname("localhost")
                        .setMapPort(80)
        ), now);
        c1.setIp("1.1.1.1");
        c1.setStatus(ContainerStatusDto.CHECKING);
        c1.setNeedsNewGpu(false);
        containerStorage.addContainer(c1, userId, courseId);

        // 2. Schedule task for C1
        ContainerScheduledDto cs1 = new ContainerScheduledDto()
                .setContainerDto(c1)
                .setUserId(userId)
                .setCourseId(courseId);

        // 3. Create container C2 and add it to storage (overwriting C1)
        ContainerDto c2 = new ContainerDto("user-1", "password", List.of(
                (ContainerPortDto) new ContainerPortDto()
                        .setPortMapTo(8001)
                        .setConnectionType("HTTPS")
                        .setRequiredToAccessContainer(true)
                        .setHostname("localhost")
                        .setMapPort(80)
        ), now);
        c2.setIp("2.2.2.2");
        c2.setStatus(ContainerStatusDto.CHECKING);
        c2.setNeedsNewGpu(false);
        containerStorage.addContainer(c2, userId, courseId);

        // Verify storage has C2
        assertEquals(c2, containerStorage.getContainer(userId, courseId).orElse(null));

        // 4. Run Task for C1
        // We mock some calls to make sure it reaches the end
        when(configuration.getNumberOfConnectionRetry()).thenReturn(5L);
        when(nodeIPRequestService.getNodeIP(anyString(), anyString())).thenReturn("1.1.1.1");

        ContainerTestConnectionTask task = new ContainerTestConnectionTask(
                containerStorage,
                scheduler,
                cs1,
                configuration,
                nodeIPRequestService,
                testSocket,
                containerStatusConfigureService
        );
        task.run();

        // 5. Assert that ContainerStorage contains C2
        // If the bug exists, storage will contain C1 because task.run() overwrote it.
        ContainerDto containerInStorage = containerStorage.getContainer(userId, courseId).get();
        assertEquals(c2.getIp(), containerInStorage.getIp());
        assertNotEquals(c1.getIp(), containerInStorage.getIp());
    }

    @Test
    void regressionTestForRaceConditionWithSamePorts() {
        String userId = "user2";
        String courseId = "course2";
        LocalDateTime now = LocalDateTime.now();
        List<ContainerPortDto> ports = List.of(
                (ContainerPortDto) new ContainerPortDto()
                        .setPortMapTo(8001)
                        .setConnectionType("HTTPS")
                        .setRequiredToAccessContainer(false)
                        .setHostname("localhost")
                        .setMapPort(80)
        );
        
        // 1. Create container C1 and add it to storage
        ContainerDto c1 = new ContainerDto("user-2", "password", ports, now);
        c1.setIp("1.1.1.1");
        c1.setStatus(ContainerStatusDto.CHECKING);
        c1.setNeedsNewGpu(false);
        containerStorage.addContainer(c1, userId, courseId);

        // 2. Schedule task for C1
        ContainerScheduledDto cs1 = new ContainerScheduledDto()
                .setContainerDto(c1)
                .setUserId(userId)
                .setCourseId(courseId);

        // 3. Create container C2 and add it to storage (overwriting C1)
        ContainerDto c2 = new ContainerDto("user-2", "password", ports, now);
        c2.setIp("2.2.2.2");
        c2.setStatus(ContainerStatusDto.CHECKING);
        c2.setNeedsNewGpu(false);
        containerStorage.addContainer(c2, userId, courseId);

        // Verify storage has C2
        assertEquals(c2.getIp(), containerStorage.getContainer(userId, courseId).get().getIp());
        assertNotEquals(c1.getIp(), containerStorage.getContainer(userId, courseId).get().getIp());

        // 4. Run Task for C1
        // We mock some calls to make sure it reaches the end
        when(configuration.getNumberOfConnectionRetry()).thenReturn(5L);
        when(nodeIPRequestService.getNodeIP(anyString(), anyString())).thenReturn("1.1.1.1");

        ContainerTestConnectionTask task = new ContainerTestConnectionTask(
                containerStorage,
                scheduler,
                cs1,
                configuration,
                nodeIPRequestService,
                testSocket,
                containerStatusConfigureService
        );
        task.run();

        // 5. Assert that ContainerStorage contains C1 because C1 and C2 are "equal" so C1 task overwrites C2
        ContainerDto containerInStorage = containerStorage.getContainer(userId, courseId).get();
        assertEquals(c1.getIp(), containerInStorage.getIp());
        assertNotEquals(c2.getIp(), containerInStorage.getIp());
    }

    @Test
    void testTaskUpdatesStorageWhenContainerMatches() {
        String userId = "user1";
        String courseId = "course1";

        // 1. Create container C1 and add it to storage
        ContainerDto c1 = new ContainerDto("user-1", "password", new ArrayList<>(), LocalDateTime.now());
        c1.setIp("1.1.1.1");
        c1.setStatus(ContainerStatusDto.CHECKING);
        c1.setNeedsNewGpu(false);
        containerStorage.addContainer(c1, userId, courseId);

        // 2. Schedule task for C1
        ContainerScheduledDto cs1 = new ContainerScheduledDto()
                .setContainerDto(c1)
                .setUserId(userId)
                .setCourseId(courseId);

        // 3. Mock dependencies
        when(configuration.getNumberOfConnectionRetry()).thenReturn(5L);
        when(nodeIPRequestService.getNodeIP(anyString(), anyString())).thenReturn("1.1.1.1");

        // 4. Run Task
        ContainerTestConnectionTask task = new ContainerTestConnectionTask(
                containerStorage,
                scheduler,
                cs1,
                configuration,
                nodeIPRequestService,
                testSocket,
                containerStatusConfigureService
        );
        task.run();

        // 5. Assert storage is updated (status should be OK because testAllConnection defaults to true for empty ports)
        ContainerDto stored = containerStorage.getContainer(userId, courseId).orElse(null);
        assertEquals(stored, c1);
        assertEquals(ContainerStatusDto.OK, stored.getStatus());
    }


    @Test
    void regressionTestForRaceConditionWithDifferentStatus() {
        String userId = "user2";
        String courseId = "course2";
        LocalDateTime now = LocalDateTime.now();
        // 1. Create container C1 and add it to storage
        ContainerDto c1 = new ContainerDto("user-2", "password", new ArrayList<>(), now);
        c1.setIp("1.1.1.1");
        c1.setStatus(ContainerStatusDto.PENDING);
        c1.setNeedsNewGpu(false);
        containerStorage.addContainer(c1, userId, courseId);

        // 2. Schedule task for C1
        ContainerScheduledDto cs1 = new ContainerScheduledDto()
                .setContainerDto(c1)
                .setUserId(userId)
                .setCourseId(courseId);

        // 3. Create container C2 and add it to storage (overwriting C1)
        ContainerDto c2 = new ContainerDto("user-2", "password", new ArrayList<>(), now);
        c2.setIp("2.2.2.2");
        c2.setStatus(ContainerStatusDto.CHECKING);
        c2.setNeedsNewGpu(false);
        containerStorage.addContainer(c2, userId, courseId);

        // Verify storage has C2
        assertEquals(c2.getIp(), containerStorage.getContainer(userId, courseId).get().getIp());
        assertNotEquals(c1.getIp(), containerStorage.getContainer(userId, courseId).get().getIp());

        // 4. Run Task for C1
        // We mock some calls to make sure it reaches the end
        when(configuration.getNumberOfConnectionRetry()).thenReturn(5L);
        when(nodeIPRequestService.getNodeIP(anyString(), anyString())).thenReturn("1.1.1.1");

        ContainerTestConnectionTask task = new ContainerTestConnectionTask(
                containerStorage,
                scheduler,
                cs1,
                configuration,
                nodeIPRequestService,
                testSocket,
                containerStatusConfigureService
        );
        task.run();

        // 5. Assert that ContainerStorage contains C1 because C1 and C2 are "equal" (status is not included in the equality check) 
        // so C1 task overwrites C2
        ContainerDto containerInStorage = containerStorage.getContainer(userId, courseId).get();
        assertEquals(c1.getIp(), containerInStorage.getIp());
        assertNotEquals(c2.getIp(), containerInStorage.getIp());
    }
}
