package fr.centralesupelec.thuv.web.admin;

import fr.centralesupelec.thuv.dtos.ComputeTypeUpdateDto;
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

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping(value = "/")
    public List<ComputeType> getComputeTypes() {
        return computeTypeRepository.findAll(Sort.by("id"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/{id}")
    public ComputeType getComputeType(
            @PathVariable("id") ComputeType computeType
    ) {
        return computeType;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "")
    public ComputeType newComputeType(
            @RequestBody @Valid ComputeTypeUpdateDto dto
    ) {
        ComputeType computeType = new ComputeType();
        return computeTypeService.updateComputeType(dto, computeType);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/{id}")
    public ComputeType editComputeType(
            @PathVariable("id") ComputeType computeType,
            @RequestBody @Valid ComputeTypeUpdateDto dto
    ) {
        return computeTypeService.updateComputeType(dto, computeType);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(value = "/{id}")
    public void deleteComputeType(
            @PathVariable("id") ComputeType computeType
    ) {
        computeTypeService.deleteComputeType(computeType);
    }
}
