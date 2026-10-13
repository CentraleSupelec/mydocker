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

    /**
     * hasAdminAuthority comes from the session, not from the user row: an account may hold
     * ROLE_ADMIN in the database while the session it is using was opened through a provider that
     * does not grant administrator authority.
     */
    public Page<DockerImage> getViewableDockerImage(
            User user, boolean hasAdminAuthority, String search, Pageable pageable
    ) {
        if (hasAdminAuthority) {
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
