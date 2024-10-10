package fr.centralesupelec.thuv.repository;

import fr.centralesupelec.thuv.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.constraints.NotBlank;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String email);
    Optional<User> findByEnabledTrueAndUsername(String email);
    List<User> findByEnabledTrueAndEmail(String email);
    List<User> findDistinctByEmailContainingAndRolesNameIsNot(@NotBlank String email, String rolesName);
    Page<User> findDistinctByEmailContainingAndRolesNameIn(
            @NotBlank String email, Collection<String> rolesName, Pageable pageable
    );
    Page<User> findDistinctByRolesNameIn(Collection<String> rolesName, Pageable pageable);
    boolean existsByUsername(String username);
}
