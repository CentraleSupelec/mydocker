package fr.centralesupelec.thuv.permissions.repository;

import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.permissions.models.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    List<Permission> findByUser(@NotNull User user);
}
