package fr.centralesupelec.thuv.web.student;

import fr.centralesupelec.thuv.model.Content;
import fr.centralesupelec.thuv.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;


@RestController("contentController")
@RequestMapping("content")
@RequiredArgsConstructor
public class ContentController {
    private final ContentRepository contentRepository;

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{slug}")
    public ResponseEntity<Content> getContent(@PathVariable String slug) {
        Optional<Content> contentOptional = contentRepository.findBySlug(slug);
        if (contentOptional.isPresent() && contentOptional.get().getEnabled()) {
            return ResponseEntity.ok(contentOptional.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
