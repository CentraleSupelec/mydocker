package fr.centralesupelec.thuv.docker_build.repository;

import fr.centralesupelec.thuv.docker_build.model.DockerImage;
import fr.centralesupelec.thuv.docker_build.model.DockerImage_;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.permissions.models.DockerImagePermission;
import fr.centralesupelec.thuv.permissions.models.Permission_;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.domain.Specification;

public class DockerImageSearchSpecifications {
    public static Specification<DockerImage> viewableForUser(@NotNull User user) {
        return (root, cq, cb) -> {
            Join<DockerImage, DockerImagePermission> permission = root.join(DockerImage_.PERMISSIONS, JoinType.LEFT);
            cq.where(
                    cb.or(
                            cb.equal(root.get(DockerImage_.CREATOR), user),
                            cb.equal(permission.get(Permission_.USER), user),
                            cb.isTrue(root.get(DockerImage_.VISIBLE))
                    )
            );
            return cq.getRestriction();
        };
    }

    public static Specification<DockerImage> viewableForUserByNameContainingIgnoreCase(
            @NotNull User user, String name
    ) {
        return (root, cq, cb) -> {
            Join<DockerImage, DockerImagePermission> permission = root.join(DockerImage_.PERMISSIONS, JoinType.LEFT);
            cq.where(
                    cb.and(
                            cb.or(
                                    cb.equal(root.get(DockerImage_.CREATOR), user),
                                    cb.equal(permission.get(Permission_.USER), user),
                                    cb.isTrue(root.get(DockerImage_.VISIBLE))
                            ),
                            cb.like(cb.lower(root.get(DockerImage_.NAME)), ("%" + name + "%").toLowerCase())
                    )
            );
            return cq.getRestriction();
        };
    }
}
