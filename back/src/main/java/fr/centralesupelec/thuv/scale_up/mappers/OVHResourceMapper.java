package fr.centralesupelec.thuv.scale_up.mappers;

import fr.centralesupelec.thuv.scale_up.dtos.OVHResourceDto;
import fr.centralesupelec.thuv.scale_up.model.OVHResource;
import org.springframework.stereotype.Service;

@Service
public class OVHResourceMapper {
    public OVHResourceDto convertToDto(OVHResource ovhResource) {
        return new OVHResourceDto()
                .setId(
                        ovhResource.getId()
                )
                .setType(
                        ovhResource.getType()
                )
                .setRamInGo(
                        ovhResource.getRamInGo()
                )
                .setCoreNumber(
                        ovhResource.getCoreNumber()
                );
    }
}
