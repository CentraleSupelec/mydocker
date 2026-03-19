package fr.centralesupelec.thuv.dtos;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class ContentUpdateDto {
    @NotNull
    private String title;
    @NotNull
    private String richText;
    private boolean enabled = false;
}
