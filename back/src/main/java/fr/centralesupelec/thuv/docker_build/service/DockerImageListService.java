package fr.centralesupelec.thuv.docker_build.service;

import fr.centralesupelec.thuv.docker_build.model.DockerImage;
import fr.centralesupelec.thuv.docker_build.repository.DockerImageRepository;
import fr.centralesupelec.thuv.docker_build.repository.DockerImageSearchSpecifications;
import fr.centralesupelec.thuv.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DockerImageListService {
    /**
     * Not an attribute of DockerImage: the latest updatedOn of its builds, sorted inside the query.
     */
    public static final String LAST_BUILD_ON = "lastBuildOn";

    private final DockerImageRepository dockerImageRepository;

    /**
     * hasAdminAuthority comes from the session, not from the user row: an account may hold
     * ROLE_ADMIN in the database while the session it is using was opened through a provider that
     * does not grant administrator authority.
     */
    public Page<DockerImage> getViewableDockerImage(
            User user, boolean hasAdminAuthority, String search, Pageable pageable
    ) {
        Sort.Order lastBuildOn = pageable.getSort().getOrderFor(LAST_BUILD_ON);
        if (lastBuildOn != null) {
            return getViewableDockerImageByLastBuildOn(user, hasAdminAuthority, search, pageable, lastBuildOn);
        }

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

    private Page<DockerImage> getViewableDockerImageByLastBuildOn(
            User user, boolean hasAdminAuthority, String search, Pageable pageable, Sort.Order lastBuildOn
    ) {
        Sort thenBy = Sort.by(
                pageable.getSort().filter(order -> !order.getProperty().equals(LAST_BUILD_ON)).toList()
        );
        Specification<DockerImage> filter;
        if (hasAdminAuthority) {
            filter = search != null && !search.isEmpty()
                    ? DockerImageSearchSpecifications.nameContainingIgnoreCase(search)
                    : Specification.unrestricted();
        } else {
            filter = search != null && !search.isEmpty()
                    ? DockerImageSearchSpecifications.viewableForUserByNameContainingIgnoreCase(user, search)
                    : DockerImageSearchSpecifications.viewableForUser(user);
        }
        Pageable unsorted = pageable.isPaged()
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())
                : Pageable.unpaged();
        return dockerImageRepository.findAll(
                filter.and(DockerImageSearchSpecifications.orderByLastBuildOn(lastBuildOn.getDirection(), thenBy)),
                unsorted
        );
    }
}
