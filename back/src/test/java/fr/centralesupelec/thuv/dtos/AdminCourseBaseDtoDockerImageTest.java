package fr.centralesupelec.thuv.dtos;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminCourseBaseDtoDockerImageTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static Set<ConstraintViolation<AdminCourseBaseDto>> validate(String image) {
        AdminCourseBaseDto dto = new AdminCourseBaseDto();
        dto.setDockerImage(image);
        return validator.validateProperty(dto, "dockerImage");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "nginx",
            "nginx:1.27",
            "gitlab-research.centralesupelec.fr:4567/my-docker-images/vnc-igv:latest",
            "registry.local/a/b/c:1.0",
            "localhost:5000/x",
            "nginx@sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            "nginx:1.27@sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    })
    void acceptsWellFormedReferences(String image) {
        assertTrue(validate(image).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            " nginx",
            "nginx ",
            "my image:1",
            "UPPER/Case",
            "",
            "gitlab-research.centralesupelec.fr:4567/my-docker-images/vnc-igv "
    })
    void refusesMalformedReferences(String image) {
        Set<ConstraintViolation<AdminCourseBaseDto>> violations = validate(image);
        assertEquals(false, violations.isEmpty());
        assertTrue(violations.stream().allMatch(v -> v.getPropertyPath().toString().equals("dockerImage")));
    }
}
