package fr.centralesupelec.thuv.repository;

import fr.centralesupelec.thuv.model.Content;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContentRepository extends JpaRepository<Content, Long> {
    boolean existsBySlug(String slug);
    Optional<Content> findBySlug(@NotBlank String slug);
}
