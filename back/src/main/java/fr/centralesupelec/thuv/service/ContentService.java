package fr.centralesupelec.thuv.service;

import fr.centralesupelec.thuv.dtos.ContentUpdateDto;
import fr.centralesupelec.thuv.model.Content;
import fr.centralesupelec.thuv.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContentService {
    private final ContentRepository contentRepository;

    public Content createContent(ContentUpdateDto dto, Content content) {
        content.setSlug(generateUniqueSlug(dto.getTitle()));
        this.updateContent(dto, content);
        return content;
    }

    public Content updateContent(ContentUpdateDto dto, Content content) {
        content
            .setTitle(dto.getTitle())
            .setRichText(dto.getRichText())
            .setEnabled(dto.isEnabled());
        this.contentRepository.saveAndFlush(content);

        return content;
    }

    public static String toSlug(String input) {
        if (input == null) {
            return "";
        }

        String slug = java.text.Normalizer.normalize(input.toLowerCase(), java.text.Normalizer.Form.NFD);
        slug = slug.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        slug = slug.replaceAll("[^a-z0-9\\s-]", "");
        slug = slug.replaceAll("[\\s-]+", "-");
        slug = slug.replaceAll("^-+|-+$", "");
        return slug;
    }

    public String generateUniqueSlug(String title) {
        String baseSlug = toSlug(title);
        String slug = baseSlug;
        int count = 1;

        while (contentRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + count;
            count++;
        }

        return slug;
    }


    public void deleteContent(Content content) {
        this.contentRepository.delete(content);
    }
}
