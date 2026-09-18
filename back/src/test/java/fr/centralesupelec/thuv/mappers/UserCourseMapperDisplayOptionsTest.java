package fr.centralesupelec.thuv.mappers;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.centralesupelec.thuv.dtos.UserCourseDto;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.User;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * display_options is a nullable column, so the mapper is handed every shape the database
 * allows. A null used to escape as IllegalArgumentException from Jackson, which is not the
 * JsonProcessingException the mapper caught, and one such row made GET /courses/joined answer
 * 500 for every student enrolled in that course. The mapper's intent is to degrade to an empty
 * map; these cases hold it to that for all three shapes.
 */
class UserCourseMapperDisplayOptionsTest {

    private final UserCourseMapper mapper = new UserCourseMapper(
            mock(PortsMapper.class),
            new ObjectMapper(),
            mock(SessionMapper.class),
            ZoneId.of("Europe/Paris")
    );

    private Course courseWithDisplayOptions(String displayOptions) {
        Course course = new Course();
        course.setTitle("a course");
        course.setCreator(new User());
        course.setDisplayOptions(displayOptions);
        return course;
    }

    @Test
    void nullDisplayOptionsBecomeAnEmptyMap() {
        UserCourseDto dto = mapper.convertToDto(courseWithDisplayOptions(null));

        assertTrue(dto.getDisplayOptions().isEmpty());
    }

    @Test
    void blankDisplayOptionsBecomeAnEmptyMap() {
        UserCourseDto dto = mapper.convertToDto(courseWithDisplayOptions(""));

        assertTrue(dto.getDisplayOptions().isEmpty());
    }

    @Test
    void malformedDisplayOptionsBecomeAnEmptyMap() {
        UserCourseDto dto = mapper.convertToDto(courseWithDisplayOptions("{not json"));

        assertTrue(dto.getDisplayOptions().isEmpty());
    }

    @Test
    void validDisplayOptionsAreKept() {
        UserCourseDto dto = mapper.convertToDto(courseWithDisplayOptions("{\"hideLogs\":true}"));

        assertEquals(1, dto.getDisplayOptions().size());
        assertEquals(true, dto.getDisplayOptions().get("hideLogs"));
    }
}
