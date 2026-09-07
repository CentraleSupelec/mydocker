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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UpdateCourseMapper {
    /**
     * Canonical grammar for the port placeholder a course may use in its launch command:
     * <pre>{@code {{PORT['<port>']}}   or   {{PORT["<port>"]}}}</pre>
     * The opening and closing quotes must match and the body must be a port written without a
     * leading zero. Leading zeros are refused rather than normalised: the Go side would resolve
     * "08080" numerically to 8080 while this validator compares it as text against the declared
     * ports and sees an unknown one, so the three would disagree about the same command. The front-end
     * validator and the Go substitution implement the same grammar, so a change here belongs
     * in all three at once. Anything that opens with {@code {{PORT[} and does not match is
     * malformed: the Go side would leave it in the command as a literal, so it is rejected
     * here rather than shipped to a container.
     * <p>
     * Validation works by candidate rather than by shape: every occurrence of the opening
     * {@code {{PORT[} is a candidate, and a candidate that is not a canonical placeholder
     * starting at that exact offset is malformed. Matching a "malformed shape" instead would
     * miss the unterminated cases, {@code {{PORT[8080} and {@code {{PORT['8080']}} and a bare
     * {@code {{PORT[}.
     */
    private static final Pattern COMMAND_PORT_PATTERN =
            Pattern.compile("\\{\\{PORT\\[(?:'([1-9][0-9]*)'|\"([1-9][0-9]*)\")\\]\\}\\}");
    private static final String COMMAND_PORT_PREFIX = "{{PORT[";
    private static final int MAX_PORT = 65535;
    /** Digits of the highest port, so a longer run can be refused without parsing it. */
    private static final int MAX_PORT_DIGITS = 5;
    /** How much of a malformed candidate to quote back to the user. */
    private static final int CANDIDATE_EXCERPT_LENGTH = 32;
    private final PortsMapper portsMapper;
    private final SessionMapper sessionMapper;
    private final ObjectMapper objectMapper;
    private final ComputeTypeRepository computeTypeRepository;
    private final ZoneId zoneId;


    public void updateChanges(Course course, AdminUpdateCourseDto dto) {
        validateCommandPorts(dto);
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

    /**
     * The grammar bounds the port by value as well as by shape. Without this, a course could
     * save {@code {{PORT['4294975376']}}}, which the Go side narrowed to a uint32 and resolved
     * as 8080, substituting a port nobody asked for.
     */
    private boolean isWithinPortRange(String port) {
        return port.length() <= MAX_PORT_DIGITS && Integer.parseInt(port) <= MAX_PORT;
    }

    private void validateCommandPorts(AdminUpdateCourseDto dto) {
        String command = dto.getCommand();
        if (command == null || command.isBlank()) {
            return;
        }

        Set<String> validPortKeys = new HashSet<>();
        if (dto.getPorts() != null) {
            dto.getPorts().forEach(port -> {
                if (port.getMapPort() != null) {
                    validPortKeys.add(port.getMapPort().toString());
                }
            });
        }

        List<String> malformedPlaceholders = new ArrayList<>();
        List<String> referencedPorts = new ArrayList<>();
        Matcher matcher = COMMAND_PORT_PATTERN.matcher(command);

        for (
                int index = command.indexOf(COMMAND_PORT_PREFIX);
                index >= 0;
                index = command.indexOf(COMMAND_PORT_PREFIX, index + COMMAND_PORT_PREFIX.length())
        ) {
            String port = null;
            if (matcher.find(index) && matcher.start() == index) {
                port = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            }

            if (port != null && isWithinPortRange(port)) {
                referencedPorts.add(port);
            } else {
                malformedPlaceholders.add(
                        command.substring(
                                index,
                                Math.min(index + CANDIDATE_EXCERPT_LENGTH, command.length())
                        )
                );
            }
        }

        if (!malformedPlaceholders.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format(
                            "Malformed port placeholder(s) in command: %s. Expected {{PORT['8080']}}.",
                            String.join(", ", malformedPlaceholders)
                    )
            );
        }

        List<String> invalidPorts = new ArrayList<>();
        for (String referencedPort : referencedPorts) {
            if (!validPortKeys.contains(referencedPort)) {
                invalidPorts.add(referencedPort);
            }
        }

        if (!invalidPorts.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("Unknown port(s) referenced in command: %s", String.join(", ", invalidPorts))
            );
        }
    }
}
