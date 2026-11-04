package fr.centralesupelec.thuv.dtos;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminCourseBaseDtoRunUidTest {
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

    private static Set<ConstraintViolation<AdminCourseBaseDto>> validate(String runUid) {
        AdminCourseBaseDto dto = new AdminCourseBaseDto();
        dto.setRunUid(runUid);
        return validator.validateProperty(dto, "runUid");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1000", "1000:100", "57439:57439"})
    void acceptsEmptyOrPositiveIds(String runUid) {
        assertTrue(validate(runUid).isEmpty());
    }

    @Test
    void acceptsNull() {
        assertTrue(validate(null).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0:0", "1000:0", "0:100", "01000", "1000:0100", "1000:", ":100", "1000:100:", "-1", "abc", " 1000", "1000; reboot"})
    void refusesZeroLeadingZerosAndMalformedValues(String runUid) {
        Set<ConstraintViolation<AdminCourseBaseDto>> violations = validate(runUid);
        assertEquals(false, violations.isEmpty());
        assertTrue(violations.stream().allMatch(v -> v.getPropertyPath().toString().equals("runUid")));
    }
}
