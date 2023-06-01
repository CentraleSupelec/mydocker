package fr.centralesupelec.thuv.repository;

import fr.centralesupelec.thuv.model.Role;
import fr.centralesupelec.thuv.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
    Role getByName(String name);
    Optional<Collection<Role>> findByUsers(User user);
}
