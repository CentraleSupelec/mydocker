package fr.centralesupelec.thuv.docker_build.service;

import fr.centralesupelec.thuv.docker_build.model.DockerImage;
import fr.centralesupelec.thuv.docker_build.repository.DockerImageRepository;
import fr.centralesupelec.thuv.docker_build.repository.DockerImageSearchSpecifications;
import fr.centralesupelec.thuv.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DockerImageListService {
    private final DockerImageRepository dockerImageRepository;

    public Page<DockerImage> getViewableDockerImage(User user, String search, Pageable pageable) {
        if (user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"))) {
            if (search != null && !search.isEmpty()) {
                return dockerImageRepository.findAllByNameContainingIgnoreCase(search, pageable);
            }
            return dockerImageRepository.findAllBy(pageable);
        }

        if (search != null && !search.isEmpty()) {
            return dockerImageRepository.findAll(
                    DockerImageSearchSpecifications.viewableForUserByNameContainingIgnoreCase(user, search),
                    pageable
            );
        }
        return dockerImageRepository.findAll(DockerImageSearchSpecifications.viewableForUser(user), pageable);
    }
}
