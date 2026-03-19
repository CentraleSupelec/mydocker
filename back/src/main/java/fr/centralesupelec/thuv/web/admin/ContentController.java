package fr.centralesupelec.thuv.web.admin;

import fr.centralesupelec.thuv.dtos.ContentUpdateDto;
import fr.centralesupelec.thuv.model.Content;
import fr.centralesupelec.thuv.repository.ContentRepository;
import fr.centralesupelec.thuv.service.ContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController("adminContentController")
@RequestMapping("admin/contents")
@RequiredArgsConstructor
public class ContentController {
    private final ContentRepository contentRepository;
    private final ContentService contentService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/")
    public List<Content> getContents() {
        return contentRepository.findAll(Sort.by("id"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/{id}")
    public Content getContent(
            @PathVariable("id") Content content
    ) {
        return content;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "")
    public Content newContent(
            @RequestBody @Valid ContentUpdateDto dto
    ) {
        Content content = new Content();
        return contentService.createContent(dto, content);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/{id}")
    public Content editContent(
            @PathVariable("id") Content content,
            @RequestBody @Valid ContentUpdateDto dto
    ) {
        return contentService.updateContent(dto, content);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(value = "/{id}")
    public void deleteContent(
            @PathVariable("id") Content content
    ) {
        contentService.deleteContent(content);
    }
}
