package fr.centralesupelec.thuv.web.student;

import fr.centralesupelec.thuv.dtos.UserCourseDto;
import fr.centralesupelec.thuv.mappers.UserCourseMapper;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.CourseStatus;
import fr.centralesupelec.thuv.model.Role;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.repository.CourseRepository;
import fr.centralesupelec.thuv.repository.UserCourseRepository;
import fr.centralesupelec.thuv.repository.UserRepository;
import fr.centralesupelec.thuv.security.MyUserDetails;
import fr.centralesupelec.thuv.storage.UserCoursesStorage;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * A deleted course is gone for students: its invitation link, join, GPU check and external access
 * all refuse it.
 */
class StudentCourseControllerDeletedTest {
    private static final long USER_ID = 7L;
    private static final long COURSE_ID = 42L;
    private static final UUID COURSE_UUID = UUID.fromString("7c2f2b1e-0000-4000-8000-000000000000");

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserCourseRepository userCourseRepository = mock(UserCourseRepository.class);
    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final UserCourseMapper userCourseMapper = mock(UserCourseMapper.class);
    private final UserCoursesStorage userCoursesStorage = mock(UserCoursesStorage.class);
    private final CourseController controller = new CourseController(
            userRepository, userCourseRepository, courseRepository, userCourseMapper, userCoursesStorage
    );

    private static int statusOf(Supplier<?> call) {
        try {
            Object result = call.get();
            if (result instanceof ResponseEntity<?> response) {
                return response.getStatusCode().value();
            }
            return 200;
        } catch (ResponseStatusException e) {
            return e.getStatusCode().value();
        }
    }

    private static Course course(CourseStatus status) {
        Course course = new Course();
        course.setId(COURSE_ID);
        course.setStatus(status);
        course.setLink("recetteLink");
        return course;
    }

    private static MyUserDetails principal() {
        Role role = mock(Role.class);
        when(role.getName()).thenReturn("ROLE_USER");
        return new MyUserDetails(List.of(role), "u@example.org", USER_ID, true);
    }

    @Test
    void linkOfDeletedCourseIsGone() {
        when(courseRepository.getCourseByLink("recetteLink")).thenReturn(course(CourseStatus.DELETED));
        assertEquals(410, statusOf(() -> controller.getCourseInformations("recetteLink")));
        verifyNoInteractions(userCourseMapper);

        Course ready = course(CourseStatus.READY);
        UserCourseDto dto = new UserCourseDto();
        when(courseRepository.getCourseByLink("recetteLink")).thenReturn(ready);
        when(userCourseMapper.convertToDto(ready)).thenReturn(dto);
        ResponseEntity<UserCourseDto> response = controller.getCourseInformations("recetteLink");
        assertEquals(200, response.getStatusCode().value());
        assertSame(dto, response.getBody());
    }

    @Test
    void joinAndIsGpuOfDeletedCourseAreGone() {
        User user = mock(User.class);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        Course deleted = course(CourseStatus.DELETED);
        MyUserDetails principal = principal();

        assertEquals(410, statusOf(() -> controller.joinCourse(deleted, principal)));
        verify(userRepository, never()).save(any());
        verify(courseRepository, never()).incrementNumberOfUsers(any());
        verify(user, never()).addCourse(any());

        assertEquals(410, statusOf(() -> controller.isGpu(deleted)));
    }

    @Test
    void externalAccessClosedForDeletedCourse() {
        Course course = course(CourseStatus.DELETED);
        course.setExternalAccess(true);
        course.setExternalAccessExpirationDate(LocalDateTime.now().plusDays(1));
        when(courseRepository.findByUuid(COURSE_UUID)).thenReturn(Optional.of(course));
        assertEquals(false, controller.externalAccess(COURSE_UUID));

        course.setStatus(CourseStatus.READY);
        assertEquals(true, controller.externalAccess(COURSE_UUID));
    }
}
