package fr.centralesupelec.thuv.mappers;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.centralesupelec.thuv.dtos.AdminUpdateCourseDto;
import fr.centralesupelec.thuv.dtos.PortDto;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.repository.ComputeTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Conformance cases for the command port placeholder grammar. The Go substitution and the
 * front-end validator implement the same table; a change here belongs in all three at once.
 * Command port validation runs first in {@code updateChanges}, so these cases are reached
 * before any repository is touched. The accepted cases fall through to the compute type
 * lookup, which is what the "not rejected" assertions check for.
 */
class UpdateCourseMapperCommandPortsTest {

    private final ComputeTypeRepository computeTypeRepository = mock(ComputeTypeRepository.class);

    private final UpdateCourseMapper mapper = new UpdateCourseMapper(
            mock(PortsMapper.class),
            mock(SessionMapper.class),
            new ObjectMapper(),
            computeTypeRepository,
            ZoneId.of("Europe/Paris")
    );

    private AdminUpdateCourseDto dtoWithCommand(String command) {
        when(computeTypeRepository.findById(any())).thenReturn(Optional.empty());
        PortDto port = new PortDto();
        port.setMapPort(8080);
        AdminUpdateCourseDto dto = new AdminUpdateCourseDto();
        dto.setCommand(command);
        dto.setPorts(List.of(port));
        dto.setComputeTypeId(1L);
        return dto;
    }

    private String reasonFor(String command) {
        ResponseStatusException thrown = assertThrows(
                ResponseStatusException.class,
                () -> mapper.updateChanges(new Course(), dtoWithCommand(command))
        );
        assertNotNull(thrown.getReason());
        return thrown.getReason();
    }

    private void assertAccepted(String command) {
        String reason = reasonFor(command);
        assertTrue(
                reason.startsWith("Unable to find computeType"),
                "Expected the command to pass port validation, but it was rejected with: " + reason
        );
    }

    private void assertRejected(String command, String expectedFragment) {
        String reason = reasonFor(command);
        assertTrue(
                reason.contains(expectedFragment),
                "Expected a rejection mentioning '" + expectedFragment + "', but got: " + reason
        );
    }

    @Test
    void acceptsSingleQuotedDeclaredPort() {
        assertAccepted("serve --port {{PORT['8080']}}");
    }

    @Test
    void acceptsDoubleQuotedDeclaredPort() {
        assertAccepted("serve --port {{PORT[\"8080\"]}}");
    }

    @Test
    void acceptsCommandWithoutPlaceholder() {
        assertAccepted("sleep infinity");
    }

    @Test
    void acceptsOtherPlaceholders() {
        assertAccepted("login {{USERNAME}} {{PASSWORD}} {{IP}}");
    }

    @Test
    void rejectsUndeclaredPort() {
        assertRejected("serve --port {{PORT['9999']}}", "Unknown port(s) referenced in command: 9999");
    }

    @Test
    void rejectsUnquotedPlaceholder() {
        assertRejected("serve --port {{PORT[8080]}}", "Malformed port placeholder(s)");
    }

    @Test
    void rejectsMismatchedQuotes() {
        assertRejected("serve --port {{PORT['8080\"]}}", "Malformed port placeholder(s)");
    }

    @Test
    void rejectsNonNumericPort() {
        assertRejected("serve --port {{PORT['http']}}", "Malformed port placeholder(s)");
    }

    @Test
    void rejectsEmptyPlaceholder() {
        assertRejected("serve --port {{PORT[]}}", "Malformed port placeholder(s)");
    }
}
