package fr.centralesupelec.thuv.scale_up.mappers;

import fr.centralesupelec.thuv.mappers.SessionWithCourseMapper;
import fr.centralesupelec.thuv.scale_up.dtos.OVHRegionWorkerWithSessionDto;
import fr.centralesupelec.thuv.scale_up.model.OVHRegionWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OVHRegionWorkerWithSessionMapper {
    private final SessionWithCourseMapper sessionWithCourseMapper;

    public OVHRegionWorkerWithSessionDto convertToDto(OVHRegionWorker ovhRegionWorker) {
        OVHRegionWorkerWithSessionDto dto = (OVHRegionWorkerWithSessionDto) new OVHRegionWorkerWithSessionDto()
                .setSessions(
                        ovhRegionWorker.getLaunchDeployment().getSessionsToLaunch()
                                .stream()
                                .map(sessionWithCourseMapper::convertToDto)
                                .collect(Collectors.toList())
                )
                .setId(
                        ovhRegionWorker.getId()
                )
                .setRegion(
                        ovhRegionWorker.getRegion().getRegion()
                )
                .setCount(
                        ovhRegionWorker.getCount()
                )
                .setResource(
                        ovhRegionWorker.getRessource().getId()
                );
        if (ovhRegionWorker.getComputeType() != null) {
            dto
                    .setComputeTypeId(
                            ovhRegionWorker.getComputeType().getId()
                    );
        }
        return dto;
    }
}
