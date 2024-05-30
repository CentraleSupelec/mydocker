package fr.centralesupelec.thuv.mappers;

import fr.centralesupelec.gRPC.OwnerAutoscalingConfig;
import fr.centralesupelec.gRPC.ScalingRegion;
import fr.centralesupelec.thuv.dtos.ComputeTypeUpdateDto;
import fr.centralesupelec.thuv.model.ComputeType;
import fr.centralesupelec.thuv.model.StorageBackend;
import fr.centralesupelec.thuv.scale_up.model.OVHRegion;
import fr.centralesupelec.thuv.scale_up.model.OVHResource;
import fr.centralesupelec.thuv.scale_up.repository.OVHRegionRepository;
import fr.centralesupelec.thuv.scale_up.repository.OVHResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComputeTypeMapper {
    private final OVHRegionRepository ovhRegionRepository;
    private final OVHResourceRepository ovhResourceRepository;

    public void updateComputeType(ComputeTypeUpdateDto dto, ComputeType computeType) {
        if (
                computeType.getTechnicalName() != null
                        && computeType.getTechnicalName().isEmpty()
                        && !dto.getTechnicalName().isEmpty()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one compute type with empty technical name is needed"
            );
        }
        if (dto.getAutoscalingResource() != null) {
            Optional<OVHResource> ovhResource = ovhResourceRepository.findById(dto.getAutoscalingResource());
            if (!ovhResource.isPresent()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format("OVH Resource %s not found", dto.getAutoscalingResource())
                );
            }
            computeType.setAutoscalingResource(ovhResource.get());
        }
        computeType.getAutoscalingRegions().clear();
        dto.getAutoscalingRegions().forEach(region -> {
            Optional<OVHRegion> ovhRegion = ovhRegionRepository.findById(region);
            if (!ovhRegion.isPresent()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format("OVH Region %s not found", region)
                );
            }
            computeType.getAutoscalingRegions().add(ovhRegion.get());
        });
        if (dto.getMaxNodesCount() != null && dto.getMinIdleNodesCount() != null) {
            if (dto.getMaxNodesCount() < dto.getMinIdleNodesCount()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format(
                                "Max nodes count (%d) is lower than min idle nodes count (%d)",
                                dto.getMaxNodesCount(),
                                dto.getMinIdleNodesCount()
                        )
                );
            }
            computeType
                    .setMinIdleNodesCount(dto.getMinIdleNodesCount())
                    .setMaxNodesCount(dto.getMaxNodesCount())
            ;
        }
        if (dto.getManualNodesCount() != null) {
            computeType.setManualNodesCount(dto.getManualNodesCount());
        }
        computeType
                .setGpu(dto.isGpu())
                .setDisplayName(dto.getDisplayName())
                .setTechnicalName(dto.getTechnicalName())
                .setStorageBackend(StorageBackend.valueOf(dto.getStorageBackend()))
        ;
    }

    public OwnerAutoscalingConfig convertComputeTypeToOwner(ComputeType computeType) {
        return OwnerAutoscalingConfig
                .newBuilder()
                .setInstanceType(computeType.getAutoscalingResource().getType())
                .setMaxNodesCount(computeType.getMaxNodesCount())
                .setMinIdleNodesCount(computeType.getMinIdleNodesCount())
                .setManualNodesCount(computeType.getManualNodesCount())
                .addAllRegions(
                        computeType
                                .getAutoscalingRegions()
                                .stream()
                                .map(
                                        ovhRegion -> ScalingRegion
                                                .newBuilder()
                                                .setRegion(ovhRegion.getRegion())
                                                .setImageId(ovhRegion.getImageId())
                                                .build()
                                )
                                .collect(Collectors.toList())
                )
                .build()
                ;
    }
}
