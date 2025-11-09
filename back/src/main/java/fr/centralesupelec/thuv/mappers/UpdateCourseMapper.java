package fr.centralesupelec.thuv.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.centralesupelec.thuv.dtos.AdminUpdateCourseDto;
import fr.centralesupelec.thuv.model.ComputeType;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.repository.ComputeTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UpdateCourseMapper {
    private final PortsMapper portsMapper;
    private final SessionMapper sessionMapper;
    private final ObjectMapper objectMapper;
    private final ComputeTypeRepository computeTypeRepository;
    private final ZoneId zoneId;


    public void updateChanges(Course course, AdminUpdateCourseDto dto) {
        Optional<ComputeType> computeType = computeTypeRepository.findById(dto.getComputeTypeId());
        if (!computeType.isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("Unable to find computeType with id '%s'", dto.getComputeTypeId())
            );
        }
        course
                .setDescription(
                        dto.getDescription()
                )
                .setStatus(
                        dto.getStatus()
                )
                .setAllowStudentToSubmit(
                        dto.getAllowStudentToSubmit()
                )
                .setTitle(
                        dto.getTitle()
                )
                .setExternalAccess(dto.isExternalAccess())
                .setExternalAccessExpirationDate(
                        dto.getExternalAccessExpirationDate() == null
                                ? null 
                                : Instant.ofEpochMilli(dto.getExternalAccessExpirationDate()).atZone(zoneId).toLocalDateTime()
                )
                .setDockerImage(
                        dto.getDockerImage()
                )
                .setNanoCpusLimit(
                        dto.getNanoCpusLimit()
                )
                .setMemoryBytesLimit(
                        dto.getMemoryBytesLimit()
                )
                .setCommand(
                        dto.getCommand()
                )
                .setComputeType(
                        computeType.get()
                )
                .setSaveStudentWork(
                        dto.getSaveStudentWork()
                )
                .setWorkdirSize(
                        dto.getWorkdirSize()
                )
                .setWorkdirPath(
                        dto.getWorkdirPath()
                )
                .setVisible(
                        dto.getVisible()
                )
                .setShutdownAfterMinutes(
                        dto.getShutdownAfterMinutes()
                )
                .setWarnShutdownMinutes(
                        dto.getWarnShutdownMinutes()
                )
                .setSessions(
                        dto.getSessions().stream()
                            .map(sessionMapper::convertToModel)
                            .collect(Collectors.toSet())
                )
                .setPorts(
                    dto.getPorts().stream()
                            .map(portsMapper::convertDTOToModel)
                            .collect(Collectors.toSet())
                )
                .setUseStudentVolume(dto.getUseStudentVolume())
                .setUid(dto.getUid())
                .setStudentVolumePath(dto.getStudentVolumePath())
        ;

        try {
            course.setDisplayOptions(
                            objectMapper.writeValueAsString(
                                    dto.getDisplayOptions()
                            )
                    );
        } catch (JsonProcessingException e) {
            course.setDisplayOptions("{}");
        }
    }
}
