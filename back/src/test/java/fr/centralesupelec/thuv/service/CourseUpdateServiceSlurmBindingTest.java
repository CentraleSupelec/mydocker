package fr.centralesupelec.thuv.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.centralesupelec.thuv.dtos.AdminUpdateCourseDto;
import fr.centralesupelec.thuv.mappers.SessionMapper;
import fr.centralesupelec.thuv.mappers.UpdateCourseMapper;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.Role;
import fr.centralesupelec.thuv.model.SlurmCluster;
import fr.centralesupelec.thuv.repository.CourseRepository;
import fr.centralesupelec.thuv.security.MyUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Only an administrator binds a Slurm cluster to a course. A teacher may still edit a bound
 * course, as long as the edit carries the current binding unchanged.
 */
class CourseUpdateServiceSlurmBindingTest {
    private static final List<String> TEACHER = List.of("ROLE_TEACHER", "ROLE_USER");
    private static final List<String> ADMIN = List.of("ROLE_ADMIN", "ROLE_TEACHER", "ROLE_USER");

    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final UpdateCourseMapper updateCourseMapper = mock(UpdateCourseMapper.class);
    private final CourseUpdateService service =
            new CourseUpdateService(courseRepository, mock(SessionMapper.class), updateCourseMapper);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void login(List<String> roleNames) {
        List<Role> roles = new ArrayList<>();
        for (String name : roleNames) {
            Role role = mock(Role.class);
            when(role.getName()).thenReturn(name);
            roles.add(role);
        }
        MyUserDetails principal = new MyUserDetails(roles, "u@example.org", 7L, true);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principal, null, roleNames.toArray(new String[0])));
    }

    private Course course(Long slurmClusterId) {
        Course course = mock(Course.class);
        when(course.getSessions()).thenReturn(Set.of());
        if (slurmClusterId != null) {
            SlurmCluster cluster = new SlurmCluster();
            cluster.setId(slurmClusterId);
            when(course.getSlurmCluster()).thenReturn(cluster);
        }
        return course;
    }

    private AdminUpdateCourseDto dto(Long slurmClusterId) {
        AdminUpdateCourseDto dto = new AdminUpdateCourseDto();
        dto.setSlurmClusterId(slurmClusterId);
        return dto;
    }

    @Test
    void teacherKeepsTheCurrentBinding() {
        login(TEACHER);
        Course course = course(5L);
        AdminUpdateCourseDto dto = dto(5L);
        service.updateCourse(dto, course);
        verify(updateCourseMapper).updateChanges(course, dto);
    }

    @Test
    void teacherCannotBindACluster() {
        login(TEACHER);
        Course course = course(null);
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.updateCourse(dto(5L), course));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
        verify(courseRepository, never()).saveAndFlush(course);
    }

    @Test
    void teacherCannotUnbindACluster() {
        login(TEACHER);
        Course course = course(5L);
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.updateCourse(dto(null), course));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
    }

    @Test
    void adminBindsACluster() {
        login(ADMIN);
        Course course = course(null);
        AdminUpdateCourseDto dto = dto(5L);
        service.updateCourse(dto, course);
        verify(updateCourseMapper).updateChanges(course, dto);
    }
}
