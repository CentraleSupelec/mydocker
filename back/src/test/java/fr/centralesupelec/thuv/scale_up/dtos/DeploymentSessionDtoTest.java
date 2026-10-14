package fr.centralesupelec.thuv.scale_up.dtos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.centralesupelec.thuv.dtos.SessionWithCourseDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeploymentSessionDtoTest {
    // The form reads the session fields at the top level, as before, with the cluster beside them.
    @Test
    void serializesTheSessionFieldsFlatWithTheCluster() throws Exception {
        SessionWithCourseDto session = new SessionWithCourseDto();
        session.setId(12L);
        session.setTitle("TP 1");

        JsonNode json = new ObjectMapper().valueToTree(
                new DeploymentSessionDto().setSession(session).setCluster(3L)
        );

        assertThat(json.get("id").asLong()).isEqualTo(12L);
        assertThat(json.get("title").asText()).isEqualTo("TP 1");
        assertThat(json.get("cluster").asLong()).isEqualTo(3L);
        assertThat(json.has("session")).isFalse();
    }
}
