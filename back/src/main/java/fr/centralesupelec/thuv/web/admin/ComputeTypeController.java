package fr.centralesupelec.thuv.web.admin;

import fr.centralesupelec.thuv.dtos.ComputeTypeDto;
import fr.centralesupelec.thuv.dtos.ComputeTypeUpdateDto;
import fr.centralesupelec.thuv.mappers.ComputeTypeMapper;
import fr.centralesupelec.thuv.model.ComputeType;
import fr.centralesupelec.thuv.repository.ComputeTypeRepository;
import fr.centralesupelec.thuv.service.ComputeTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController("adminComputeTypeController")
@RequestMapping("admin/compute-types")
@RequiredArgsConstructor
public class ComputeTypeController {
    private final ComputeTypeRepository computeTypeRepository;
    private final ComputeTypeService computeTypeService;
    private final ComputeTypeMapper computeTypeMapper;

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping(value = "/")
    public List<ComputeTypeDto> getComputeTypes() {
        return computeTypeRepository.findAll(Sort.by("id"))
            .stream()
            .map(ct -> computeTypeMapper.convertToComputeTypeDto(ct))
            .toList()
        ;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/{id}")
    public ComputeTypeDto getComputeType(
            @PathVariable("id") ComputeType computeType
    ) {
        return computeTypeMapper.convertToComputeTypeDto(computeType);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "")
    public ComputeTypeDto newComputeType(
            @RequestBody @Valid ComputeTypeUpdateDto dto
    ) {
        ComputeType computeType = new ComputeType();
        // Returns the DTO rather than the entity, as the PUT below does. ComputeType owns a
        // Set<ResourceRegion> and each ResourceRegion points back at its ComputeType with no
        // Jackson back-reference, so serialising the entity recurses.
        return computeTypeMapper.convertToComputeTypeDto(
                computeTypeService.updateComputeType(dto, computeType)
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/{id}")
    public ComputeTypeDto editComputeType(
            @PathVariable("id") ComputeType computeType,
            @RequestBody @Valid ComputeTypeUpdateDto dto
    ) {
        return computeTypeMapper.convertToComputeTypeDto(computeTypeService.updateComputeType(dto, computeType));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(value = "/{id}")
    public void deleteComputeType(
            @PathVariable("id") ComputeType computeType
    ) {
        computeTypeService.deleteComputeType(computeType);
    }
}
