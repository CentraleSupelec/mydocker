package fr.centralesupelec.thuv.web.student;

import fr.centralesupelec.thuv.mappers.SaveStateMapper;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.repository.UserCourseRepository;
import fr.centralesupelec.thuv.repository.UserRepository;
import fr.centralesupelec.thuv.security.MyUserDetails;
import fr.centralesupelec.thuv.service.ContainerRequestCreatorService;
import fr.centralesupelec.thuv.service.DelayDeletionService;
import fr.centralesupelec.thuv.service.LogRequestService;
import fr.centralesupelec.thuv.service.RequestContainerService;
import fr.centralesupelec.thuv.service.SaveDataService;
import fr.centralesupelec.thuv.service.ShutdownContainerService;
import fr.centralesupelec.thuv.storage.ContainerStorage;
import fr.centralesupelec.thuv.storage.ShutdownStatusStorage;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ContainerControllerSaveDataGateTest {

    @Test
    void saveData_forbidsWhenSaveStudentWorkGateDisabled() {
        SaveDataService saveDataService = mock(SaveDataService.class);
        UserRepository userRepository = mock(UserRepository.class);

        ContainerController controller = new ContainerController(
                mock(ContainerStorage.class),
                mock(RequestContainerService.class),
                saveDataService,
                mock(UserCourseRepository.class),
                userRepository,
                mock(ContainerRequestCreatorService.class),
                new SaveStateMapper(),
                mock(LogRequestService.class),
                mock(ShutdownContainerService.class),
                mock(ShutdownStatusStorage.class),
                mock(DelayDeletionService.class),
                ZoneId.of("UTC")
        );
        ReflectionTestUtils.setField(controller, "saveStudentWorkEnabled", false);

        Course course = new Course();
        course.setAllowStudentToSubmit(true);
        course.setWorkdirSize(128);

        MyUserDetails principal = mock(MyUserDetails.class);

        ResponseEntity<?> response = controller.saveData(course, principal);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(saveDataService, userRepository);
    }
}
