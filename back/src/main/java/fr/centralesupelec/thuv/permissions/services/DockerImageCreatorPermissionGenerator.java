package fr.centralesupelec.thuv.permissions.services;

import fr.centralesupelec.thuv.docker_build.repository.DockerImageRepository;
import fr.centralesupelec.thuv.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DockerImageCreatorPermissionGenerator implements CreatorPermissionGenerator {
    private final DockerImageRepository dockerImageRepository;

    @Override
    public List<String> generatePermissions(User user) {
        return dockerImageRepository.findByCreator(user)
                .stream()
                .map(dockerImage -> "docker_image." + dockerImage.getId() + ".creator")
                .collect(Collectors.toList());
    }
}
