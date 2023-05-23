package fr.centralesupelec.thuv.scale_up.mappers;

import fr.centralesupelec.thuv.repository.ComputeTypeRepository;
import fr.centralesupelec.thuv.scale_up.dtos.OVHRegionWorkerDto;
import fr.centralesupelec.thuv.scale_up.model.OVHRegionWorker;
import fr.centralesupelec.thuv.scale_up.repository.OVHRegionRepository;
import fr.centralesupelec.thuv.scale_up.repository.OVHResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OVHRegionWorkerMapper {
    private final OVHRegionRepository ovhRegionRepository;
    private final OVHResourceRepository ovhResourceRepository;
    private final ComputeTypeRepository computeTypeRepository;
    public OVHRegionWorker convertToModel(OVHRegionWorkerDto ovhRegionWorkerDto) {
        OVHRegionWorker ovhRegionWorker = new OVHRegionWorker()
                .setId(
                        ovhRegionWorkerDto.getId()
                )
                .setRegion(
                        ovhRegionRepository.getReferenceById(
                                ovhRegionWorkerDto.getRegion()
                        )
                )
                .setRessource(
                        ovhResourceRepository.getReferenceById(
                                ovhRegionWorkerDto.getResource()
                        )
                )
                .setCount(
                        ovhRegionWorkerDto.getCount()
                )
                ;
        if (ovhRegionWorkerDto.getComputeTypeId() != null) {
            ovhRegionWorker
                    .setComputeType(
                            computeTypeRepository.getReferenceById(
                                    ovhRegionWorkerDto.getComputeTypeId()
                            )
                    )
            ;
        }
        return ovhRegionWorker;
    }
}
