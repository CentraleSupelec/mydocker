package fr.centralesupelec.thuv.permissions.mappers;

import fr.centralesupelec.thuv.permissions.models.CoursePermission;
import fr.centralesupelec.thuv.permissions.models.Permission;
import org.springframework.stereotype.Service;

import java.util.stream.Stream;

@Service
public class CoursePermissionMapper implements SubTypePermissionMapper {
    @Override
    public Stream<String> convertPermission(Permission permission) {
        if (permission instanceof CoursePermission) {
            return Stream.of(
                    "course."
                            + ((CoursePermission) permission).getCourse().getId()
                            + "." + permission.getType()
            );
        }
        return Stream.empty();
    }
}
