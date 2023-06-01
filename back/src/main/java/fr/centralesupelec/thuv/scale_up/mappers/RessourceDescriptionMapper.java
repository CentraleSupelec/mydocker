package fr.centralesupelec.thuv.scale_up.mappers;

import fr.centralesupelec.thuv.scale_up.dtos.ResourceDescriptionDto;
import fr.centralesupelec.thuv.scale_up.model.CourseSessionOVHResource;
import fr.centralesupelec.thuv.scale_up.repository.OVHResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RessourceDescriptionMapper {
    private final OVHResourceRepository ovhResourceRepository;

    @Autowired
    public RessourceDescriptionMapper(OVHResourceRepository ovhResourceRepository) {
        this.ovhResourceRepository = ovhResourceRepository;
    }

    public ResourceDescriptionDto convertToDto(CourseSessionOVHResource courseSessionOvhResource) {
        return new ResourceDescriptionDto()
                .setCount(
                        courseSessionOvhResource.getCount()
                )
                .setOvhResourceId(
                        courseSessionOvhResource.getOvhResource().getId()
                );
    }

    public CourseSessionOVHResource convertToModel(ResourceDescriptionDto dto) {
        return new CourseSessionOVHResource()
            .setOvhResource(
                ovhResourceRepository.getReferenceById(
                        dto.getOvhResourceId()
                )
        )
            .setCount(
                    dto.getCount()
            );
    }
}
