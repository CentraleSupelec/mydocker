package fr.centralesupelec.thuv.security;

import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.permissions.models.CoursePermission;
import fr.centralesupelec.thuv.permissions.models.Permission;
import fr.centralesupelec.thuv.permissions.repository.CoursePermissionRepository;
import fr.centralesupelec.thuv.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service("coursePermissionService")
@RequiredArgsConstructor
public class CoursePermissionService {
    private final UserRepository userRepository;
    private final CoursePermissionRepository coursePermissionRepository;

    public boolean canEdit(Course course) {
        MyUserDetails principal = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.getReferenceById(
                principal.getUserId()
        );
        return principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                || course.getCreator().getId().equals(user.getId())
                || hasPermissionForCourse(
                        Collections.singletonList(Permission.Type.edit), course, user
                );
    }

    public boolean canRead(Course course) {
        MyUserDetails principal = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.getReferenceById(
                principal.getUserId()
        );
        return principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                || course.getCreator().getId().equals(user.getId())
                || hasPermissionForCourse(
                        Arrays.asList(Permission.Type.view, Permission.Type.edit), course, user
                );
    }

    public boolean canEditPermission(Course course) {
        MyUserDetails principal = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.getReferenceById(
                principal.getUserId()
        );
        return principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                || course.getCreator().getId().equals(user.getId());
    }

    private boolean hasPermissionForCourse(List<Permission.Type> types, Course course, User user) {
        Optional<CoursePermission> dockerImagePermissionOptional = coursePermissionRepository
                .findByUserAndCourse(user, course);
        return dockerImagePermissionOptional.isPresent()
                && types.contains(dockerImagePermissionOptional.get().getType());
    }
}
