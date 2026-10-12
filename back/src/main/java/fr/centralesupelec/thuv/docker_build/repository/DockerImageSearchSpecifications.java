package fr.centralesupelec.thuv.docker_build.repository;

import fr.centralesupelec.thuv.docker_build.model.DockerImage;
import fr.centralesupelec.thuv.docker_build.model.DockerImageBuild;
import fr.centralesupelec.thuv.docker_build.model.DockerImageBuild_;
import fr.centralesupelec.thuv.docker_build.model.DockerImage_;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.permissions.models.DockerImagePermission;
import fr.centralesupelec.thuv.permissions.models.DockerImagePermission_;
import fr.centralesupelec.thuv.permissions.models.Permission_;
import jakarta.persistence.criteria.CommonAbstractCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.validation.constraints.NotNull;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DockerImageSearchSpecifications {
    public static Specification<DockerImage> viewableForUser(@NotNull User user) {
        return (root, cq, cb) -> {
            cq.where(
                    viewableBy(user, root, cq, cb)
            );
            return cq.getRestriction();
        };
    }

    public static Specification<DockerImage> viewableForUserByNameContainingIgnoreCase(
            @NotNull User user, String name
    ) {
        return (root, cq, cb) -> {
            cq.where(
                    cb.and(
                            viewableBy(user, root, cq, cb),
                            cb.like(cb.lower(root.get(DockerImage_.NAME)), ("%" + name + "%").toLowerCase())
                    )
            );
            return cq.getRestriction();
        };
    }

    public static Specification<DockerImage> nameContainingIgnoreCase(String name) {
        return (root, cq, cb) -> cb.like(cb.lower(root.get(DockerImage_.NAME)), ("%" + name + "%").toLowerCase());
    }

    /**
     * Orders by the latest updatedOn of the image's builds, whatever their status; images with no
     * build come last in both directions. The order is set on the query itself, so the pageable
     * handed to the repository must be unsorted; Spring Data drops it from the count query.
     * thenBy is applied after it, then the id keeps pagination stable.
     */
    public static Specification<DockerImage> orderByLastBuildOn(Sort.Direction direction, Sort thenBy) {
        return (root, cq, cb) -> {
            Subquery<LocalDateTime> lastBuildOn = cq.subquery(LocalDateTime.class);
            Root<DockerImageBuild> build = lastBuildOn.from(DockerImageBuild.class);
            lastBuildOn.select(cb.greatest(build.get(DockerImageBuild_.updatedOn)))
                    .where(cb.equal(build.get(DockerImageBuild_.dockerImage), root));

            HibernateCriteriaBuilder hcb = (HibernateCriteriaBuilder) cb;
            List<Order> orders = new ArrayList<>();
            orders.add(direction.isAscending() ? hcb.asc(lastBuildOn, false) : hcb.desc(lastBuildOn, false));
            orders.addAll(QueryUtils.toOrders(thenBy, root, cb));
            orders.add(cb.desc(root.get(DockerImage_.id)));
            cq.orderBy(orders);
            return null;
        };
    }

    /**
     * Creator, shared through a permission, or public. The permission is checked with an exists
     * subquery rather than a join, so an image shared with several people comes back once.
     */
    private static Predicate viewableBy(
            User user, Root<DockerImage> root, CommonAbstractCriteria cq, CriteriaBuilder cb
    ) {
        Subquery<Long> sharedWithUser = cq.subquery(Long.class);
        Root<DockerImagePermission> permission = sharedWithUser.from(DockerImagePermission.class);
        sharedWithUser.select(permission.get(Permission_.id))
                .where(
                        cb.equal(permission.get(DockerImagePermission_.dockerImage), root),
                        cb.equal(permission.get(Permission_.user), user)
                );
        return cb.or(
                cb.equal(root.get(DockerImage_.CREATOR), user),
                cb.exists(sharedWithUser),
                cb.isTrue(root.get(DockerImage_.VISIBLE))
        );
    }
}
