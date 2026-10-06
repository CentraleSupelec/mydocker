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
    public static final String LOGIN_ANNOUNCEMENT_SLUG = "login-announcement";
    public static final String LOGIN_ANNOUNCEMENT_PATH = "/content/" + LOGIN_ANNOUNCEMENT_SLUG;

    private final ContentRepository contentRepository;

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{slug}")
    public ResponseEntity<Content> getContent(@PathVariable String slug) {
        return findEnabled(slug);
    }

    // Shown on the login page, so readable without being logged in. The literal path takes
    // precedence over "/{slug}"; SecurityConfig permits this exact path and nothing else.
    @GetMapping("/" + LOGIN_ANNOUNCEMENT_SLUG)
    public ResponseEntity<Content> getLoginAnnouncement() {
        return findEnabled(LOGIN_ANNOUNCEMENT_SLUG);
    }

    private ResponseEntity<Content> findEnabled(String slug) {
        Optional<Content> contentOptional = contentRepository.findBySlug(slug);
        if (contentOptional.isPresent() && contentOptional.get().getEnabled()) {
            return ResponseEntity.ok(contentOptional.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
