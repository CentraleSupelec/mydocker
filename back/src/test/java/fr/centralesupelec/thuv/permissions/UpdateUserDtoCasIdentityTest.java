package fr.centralesupelec.thuv.permissions;

import fr.centralesupelec.thuv.permissions.dtos.UpdateUserDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CAS login stores the bare principal (for example "casteacher") in both username and email, so
 * the admin form sends that value back unchanged. The update payload must accept it: a 400 here
 * is what stops an administrator from changing the role of a CAS-provisioned user.
 */
class UpdateUserDtoCasIdentityTest {
    private static final String CAS_PRINCIPAL = "casteacher";

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

    private static UpdateUserDto casPayload() {
        return new UpdateUserDto()
                .setEmail(CAS_PRINCIPAL)
                .setUsername(CAS_PRINCIPAL)
                .setName("Cas")
                .setLastname("Teacher")
                .setRoles(List.of("ROLE_TEACHER"));
    }

    @Test
    void usernameThatIsABarePrincipalIsAccepted() {
        Set<ConstraintViolation<UpdateUserDto>> violations =
                validator.validateProperty(casPayload(), "username");
        assertTrue(violations.isEmpty(), "username violations: " + violations);
    }

    @Test
    void emailThatIsABarePrincipalIsAccepted() {
        Set<ConstraintViolation<UpdateUserDto>> violations =
                validator.validateProperty(casPayload(), "email");
        assertTrue(violations.isEmpty(), "email violations: " + violations);
    }

    @Test
    void nameIsStillRequired() {
        UpdateUserDto dto = casPayload().setName("");
        Set<ConstraintViolation<UpdateUserDto>> violations = validator.validateProperty(dto, "name");
        assertEquals(1, violations.size(), "violations: " + violations);
    }
}
