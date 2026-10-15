package fr.centralesupelec.thuv.service;

import fr.centralesupelec.thuv.dtos.AdminUpdateCourseDto;
import fr.centralesupelec.thuv.mappers.AdminCourseMapper;
import fr.centralesupelec.thuv.mappers.SessionMapper;
import fr.centralesupelec.thuv.mappers.UpdateCourseMapper;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.CourseSession;
import fr.centralesupelec.thuv.model.CourseStatus;
import fr.centralesupelec.thuv.model.Role;
import fr.centralesupelec.thuv.model.UserCourse;
import fr.centralesupelec.thuv.repository.CourseRepository;
import fr.centralesupelec.thuv.repository.UserCourseRepository;
import fr.centralesupelec.thuv.repository.UserRepository;
import fr.centralesupelec.thuv.security.MyUserDetails;
import fr.centralesupelec.thuv.web.admin.CourseController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * A course becomes DELETED only through markDeleted, which touches nothing but the status; the
 * course form can neither create nor turn a course into a deleted one, but an administrator may
 * still save or restore a course that is already deleted.
 */
class CourseUpdateServiceDeletedTest {
    private static final long USER_ID = 7L;

    private static final List<String> TEACHER = List.of("ROLE_TEACHER", "ROLE_USER");
    private static final List<String> ADMIN = List.of("ROLE_ADMIN", "ROLE_TEACHER", "ROLE_USER");

    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final UpdateCourseMapper updateCourseMapper = mock(UpdateCourseMapper.class);
    private final CourseUpdateService service = new CourseUpdateService(
            courseRepository, mock(SessionMapper.class), updateCourseMapper
    );

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private MyUserDetails login(List<String> roleNames) {
        List<Role> roles = new ArrayList<>();
        for (String name : roleNames) {
            Role role = mock(Role.class);
            when(role.getName()).thenReturn(name);
            roles.add(role);
        }
        MyUserDetails principal = new MyUserDetails(roles, "u@example.org", USER_ID, true);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principal, null, roleNames.toArray(new String[0])));
        return principal;
    }

    private AdminUpdateCourseDto dto(CourseStatus status) {
        AdminUpdateCourseDto dto = new AdminUpdateCourseDto();
        dto.setStatus(status);
        dto.setSessions(new ArrayList<>());
        return dto;
    }

    private Course course(CourseStatus status) {
        Course course = mock(Course.class);
        when(course.getStatus()).thenReturn(status);
        when(course.getSessions()).thenReturn(new HashSet<>());
        return course;
    }

    @Test
    void markDeletedChangesOnlyTheStatus() {
        Course course = new Course();
        course.setStatus(CourseStatus.READY);
        course.setTitle("Recette");
        course.setLink("recetteLink");
        CourseSession session = new CourseSession();
        course.getSessions().add(session);
        UserCourse enrolment = new UserCourse();
        course.getUserCourses().add(enrolment);
        Set<CourseSession> sessions = course.getSessions();
        Set<UserCourse> enrolments = course.getUserCourses();

        service.markDeleted(course);

        assertEquals(CourseStatus.DELETED, course.getStatus());
        assertEquals("Recette", course.getTitle());
        assertEquals("recetteLink", course.getLink());
        assertSame(sessions, course.getSessions());
        assertEquals(Set.of(session), course.getSessions());
        assertSame(enrolments, course.getUserCourses());
        assertEquals(Set.of(enrolment), course.getUserCourses());
        verify(courseRepository, times(1)).saveAndFlush(course);
        verify(courseRepository, never()).deleteById(any());
        verifyNoInteractions(updateCourseMapper);
    }

    @Test
    void deletedOnlyThroughMarkDeleted() {
        for (List<String> roles : List.of(ADMIN, TEACHER)) {
            login(roles);
            ResponseStatusException thrown = assertThrows(
                    ResponseStatusException.class,
                    () -> service.updateCourse(dto(CourseStatus.DELETED), course(CourseStatus.READY))
            );
            assertEquals(HttpStatus.BAD_REQUEST.value(), thrown.getStatusCode().value());
        }
        verifyNoInteractions(updateCourseMapper);
        verify(courseRepository, never()).saveAndFlush(any());

        MyUserDetails principal = login(TEACHER);
        CourseController controller = new CourseController(
                mock(UserRepository.class),
                courseRepository,
                mock(UserCourseRepository.class),
                mock(AdminCourseMapper.class),
                updateCourseMapper,
                mock(CourseListService.class),
                mock(CourseUpdateService.class),
                mock(ConnectedUsersByCourseIdMapService.class),
                ZoneId.of("Europe/Paris")
        );
        ResponseStatusException thrown = assertThrows(
                ResponseStatusException.class,
                () -> controller.createCourse(dto(CourseStatus.DELETED), principal)
        );
        assertEquals(HttpStatus.BAD_REQUEST.value(), thrown.getStatusCode().value());
        verifyNoInteractions(updateCourseMapper);
        verify(courseRepository, never()).saveAndFlush(any());
    }

    @Test
    void adminRestoresOrEditsADeletedCourse() {
        login(ADMIN);
        Course course = course(CourseStatus.DELETED);

        // AdminUpdateCourseDto's equals ignores the status it inherits, so match each DTO by identity
        AdminUpdateCourseDto restore = dto(CourseStatus.READY);
        service.updateCourse(restore, course);
        verify(updateCourseMapper, times(1)).updateChanges(same(course), same(restore));

        AdminUpdateCourseDto edit = dto(CourseStatus.DELETED);
        service.updateCourse(edit, course);
        verify(updateCourseMapper, times(1)).updateChanges(same(course), same(edit));

        verify(courseRepository, times(2)).saveAndFlush(course);
    }
}
