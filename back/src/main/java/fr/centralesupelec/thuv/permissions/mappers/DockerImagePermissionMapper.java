package fr.centralesupelec.thuv.permissions.mappers;

import fr.centralesupelec.thuv.permissions.models.DockerImagePermission;
import fr.centralesupelec.thuv.permissions.models.Permission;
import org.springframework.stereotype.Service;

import java.util.stream.Stream;

@Service
public class DockerImagePermissionMapper implements SubTypePermissionMapper {
    @Override
    public Stream<String> convertPermission(Permission permission) {
        if (permission instanceof DockerImagePermission) {
            return Stream.of(
                "docker_image."
                        + ((DockerImagePermission) permission).getDockerImage().getId()
                        + "." + permission.getType()
            );
        }
        return Stream.empty();
    }
}
