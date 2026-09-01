package fr.centralesupelec.thuv.web.student;

import fr.centralesupelec.thuv.model.Content;
import fr.centralesupelec.thuv.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController("contentController")
@RequestMapping("content")
@RequiredArgsConstructor
public class ContentController {
    private final ContentRepository contentRepository;

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{slug}")
    public Content getContent(@PathVariable String slug) {
        return contentRepository.findBySlug(slug)
            .orElseThrow(() -> new RuntimeException("Content not found"));
    }
}
