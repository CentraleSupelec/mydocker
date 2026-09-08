package fr.centralesupelec.thuv.security.controllers;

import fr.centralesupelec.thuv.exception.UserUpsertException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AuthenticationExceptionHandlerTest {

    private final AuthenticationExceptionHandler handler = new AuthenticationExceptionHandler();

    @Test
    void ambiguousAccountAnswers409RatherThanAnAuthenticationFailure() {
        ResponseEntity<String> response = handler.handleAmbiguousAccount(
                new UserUpsertException("More than one account matches this login: 2 rows, ids [7731, 7981]")
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(AuthenticationExceptionHandler.AMBIGUOUS_ACCOUNT_MESSAGE, response.getBody());
    }

    @Test
    void ambiguousAccountMessageCarriesNoEmailAddress() {
        ResponseEntity<String> response = handler.handleAmbiguousAccount(
                new UserUpsertException("More than one account matches this login: 2 rows, ids [7731, 7981]")
        );

        assertFalse(response.getBody().contains("@"));
    }

    @Test
    void disabledAccountKeepsItsForbidden() {
        ResponseEntity<String> response = handler.handleDisabledException();

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Account disabled", response.getBody());
    }

    @Test
    void integrityViolationsAreNotTranslatedHere() {
        // Guard against reintroducing a blanket DataIntegrityViolationException mapping: it would
        // report unrelated foreign-key and not-null failures, from anywhere in the API, as an
        // ambiguous login. The email-uniqueness violation gets translated by the changeset that
        // creates the constraint, keyed on the constraint name.
        boolean handlesIntegrityViolations = java.util.Arrays
                .stream(AuthenticationExceptionHandler.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(org.springframework.web.bind.annotation.ExceptionHandler.class))
                .filter(java.util.Objects::nonNull)
                .flatMap(annotation -> java.util.Arrays.stream(annotation.value()))
                .anyMatch(type -> type.getName().endsWith("DataIntegrityViolationException"));

        assertFalse(handlesIntegrityViolations);
    }
}
