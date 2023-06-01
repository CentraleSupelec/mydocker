package fr.centralesupelec.thuv.mappers;

import fr.centralesupelec.thuv.dtos.SaveStateDto;
import fr.centralesupelec.thuv.model.UserCourse;
import org.springframework.stereotype.Service;

@Service
public class SaveStateMapper {
    public SaveStateDto convertToDto(UserCourse userCourse) {
        return new SaveStateDto()
                .setSavedAt(
                        userCourse.getSavedAt().toInstant().toEpochMilli()
                )
                .setLastSaveError(
                        userCourse.getLastSaveError()
                );
    }
}
