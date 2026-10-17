package fr.centralesupelec.thuv.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.CourseStatus;
import fr.centralesupelec.thuv.model.Role;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.permissions.models.CoursePermission;
import fr.centralesupelec.thuv.permissions.models.Permission;
import fr.centralesupelec.thuv.permissions.repository.CoursePermissionRepository;
import fr.centralesupelec.thuv.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A deleted course is closed to everyone but administrators: neither its creator nor the teachers
 * it is shared with or visible to may read it, edit it or change its permissions.
 */
class CoursePermissionServiceDeletedTest {
    private static final long USER_ID = 7L;
    private static final long OTHER_USER_ID = 8L;

    private static final List<String> TEACHER = List.of("ROLE_TEACHER", "ROLE_USER");
    private static final List<String> ADMIN = List.of("ROLE_ADMIN", "ROLE_TEACHER", "ROLE_USER");

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CoursePermissionRepository coursePermissionRepository = mock(CoursePermissionRepository.class);
    private final CoursePermissionService service = new CoursePermissionService(userRepository, coursePermissionRepository);

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
        MyUserDetails principal = new MyUserDetails(roles, "u@example.org", USER_ID, true);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principal, null, roleNames.toArray(new String[0])));

        User user = mock(User.class);
        when(user.getId()).thenReturn(USER_ID);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
    }

    private Course course(CourseStatus status, boolean createdByUser, boolean visible, Permission.Type share) {
        User creator = mock(User.class);
        when(creator.getId()).thenReturn(createdByUser ? USER_ID : OTHER_USER_ID);
        Course course = mock(Course.class);
        when(course.getStatus()).thenReturn(status);
        when(course.getCreator()).thenReturn(creator);
        when(course.getVisible()).thenReturn(visible);
        if (share == null) {
            when(coursePermissionRepository.findByUserAndCourse(any(), any())).thenReturn(Optional.empty());
        } else {
            CoursePermission permission = mock(CoursePermission.class);
            when(permission.getType()).thenReturn(share);
            when(coursePermissionRepository.findByUserAndCourse(any(), any())).thenReturn(Optional.of(permission));
        }
        return course;
    }

    private void assertAll(Course course, boolean read, boolean edit, boolean editPermission) {
        assertEquals(read, service.canRead(course));
        assertEquals(edit, service.canEdit(course));
        assertEquals(editPermission, service.canEditPermission(course));
    }

    @Test
    void deletedCourseClosedToNonAdmins() {
        login(TEACHER);
        assertAll(course(CourseStatus.DELETED, true, false, null), false, false, false);
        assertAll(course(CourseStatus.DELETED, false, false, Permission.Type.edit), false, false, false);
        assertAll(course(CourseStatus.DELETED, false, false, Permission.Type.view), false, false, false);
        assertAll(course(CourseStatus.DELETED, false, true, null), false, false, false);
    }

    @Test
    void deletedCourseOpenToAdminOtherStatusesUnchanged() {
        login(ADMIN);
        assertAll(course(CourseStatus.DELETED, false, false, null), true, true, true);

        login(TEACHER);
        assertAll(course(CourseStatus.READY, true, false, null), true, true, true);
        Course sharedForEdit = course(CourseStatus.READY, false, false, Permission.Type.edit);
        assertEquals(true, service.canRead(sharedForEdit));
        assertEquals(true, service.canEdit(sharedForEdit));
        Course sharedForView = course(CourseStatus.READY, false, false, Permission.Type.view);
        assertEquals(true, service.canRead(sharedForView));
    }
}
