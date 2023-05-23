package fr.centralesupelec.thuv.dtos;

import lombok.Data;

@Data
public class SaveStateDto {
        private String lastSaveError;
        private Long savedAt;
}
