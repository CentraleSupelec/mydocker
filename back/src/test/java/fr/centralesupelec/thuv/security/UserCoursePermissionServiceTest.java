package fr.centralesupelec.thuv.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.CourseSession;
import fr.centralesupelec.thuv.model.CourseStatus;
import fr.centralesupelec.thuv.model.Role;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.model.UserCourse;
import fr.centralesupelec.thuv.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class UserCoursePermissionServiceTest {
    private static final long USER_ID = 7L;
    private static final long COURSE_ID = 42L;

    private static final List<String> USER = List.of("ROLE_USER");
    private static final List<String> TEACHER = List.of("ROLE_TEACHER", "ROLE_USER");
    private static final List<String> ADMIN = List.of("ROLE_ADMIN", "ROLE_TEACHER", "ROLE_USER");

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserCoursePermissionService service = new UserCoursePermissionService(userRepository);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private CourseSession session(CourseStatus status, boolean joined, boolean block, LocalDateTime start) {
        Course course = mock(Course.class);
        when(course.getId()).thenReturn(COURSE_ID);
        when(course.getStatus()).thenReturn(status);

        User user = mock(User.class);
        List<UserCourse> userCourses = new ArrayList<>();
        if (joined) {
            UserCourse userCourse = mock(UserCourse.class);
            when(userCourse.getCourse()).thenReturn(course);
            userCourses.add(userCourse);
        }
        when(user.getUserCourses()).thenReturn(Set.copyOf(userCourses));
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);

        CourseSession courseSession = mock(CourseSession.class);
        when(courseSession.getCourse()).thenReturn(course);
        when(courseSession.getBlockContainerCreationBeforeStartTime()).thenReturn(block);
        when(courseSession.getStartDateTime()).thenReturn(start);
        return courseSession;
    }

    private void login(List<String> roleNames) {
        List<Role> roles = new ArrayList<>();
        for (String name : roleNames) {
            Role role = mock(Role.class);
            when(role.getName()).thenReturn(name);
            roles.add(role);
        }
        MyUserDetails principal = new MyUserDetails(roles, "u@example.org", USER_ID, true);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principal, null, roleNames.toArray(new String[0])));
    }

    private boolean can(CourseStatus status, List<String> roles, boolean joined) {
        CourseSession courseSession = session(status, joined, false, null);
        login(roles);
        return service.canAskContainer(courseSession);
    }

    @Test
    void archivedRefusedForEveryone() {
        assertEquals(false, can(CourseStatus.ARCHIVED, USER, true));
        assertEquals(false, can(CourseStatus.ARCHIVED, TEACHER, true));
        assertEquals(false, can(CourseStatus.ARCHIVED, ADMIN, true));
    }

    @Test
    void draftTestAndReadyAllowedForAnyoneWhoJoined() {
        for (CourseStatus status : List.of(CourseStatus.DRAFT, CourseStatus.TEST, CourseStatus.READY)) {
            assertEquals(true, can(status, USER, true));
            assertEquals(true, can(status, TEACHER, true));
            assertEquals(true, can(status, ADMIN, true));
        }
    }

    @Test
    void nullStatusRefusedForEveryone() {
        assertEquals(false, can(null, USER, true));
        assertEquals(false, can(null, TEACHER, true));
        assertEquals(false, can(null, ADMIN, true));
    }

    @Test
    void notJoinedAlwaysRefused() {
        for (CourseStatus status : CourseStatus.values()) {
            assertEquals(false, can(status, USER, false));
            assertEquals(false, can(status, TEACHER, false));
            assertEquals(false, can(status, ADMIN, false));
        }
    }

    @Test
    void readyAndTestStillHonourStartTime() {
        for (CourseStatus status : List.of(CourseStatus.READY, CourseStatus.TEST)) {
            CourseSession future = session(status, true, true, LocalDateTime.now().plusDays(1));
            login(USER);
            assertEquals(false, service.canAskContainer(future));

            CourseSession past = session(status, true, true, LocalDateTime.now().minusDays(1));
            login(USER);
            assertEquals(true, service.canAskContainer(past));
        }
    }
}
