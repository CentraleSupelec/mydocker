package fr.centralesupelec.thuv.security.controllers;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuplicateUserEmailExceptionHandlerTest {

    private final DuplicateUserEmailExceptionHandler handler = new DuplicateUserEmailExceptionHandler();

    @Test
    void translatesTheEmailConstraintIntoConflict() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "could not execute statement",
                new ConstraintViolationException(
                        "duplicate key value violates unique constraint",
                        new SQLException("duplicate key"),
                        "uk_users_email"
                )
        );

        ResponseEntity<String> response = handler.handleDuplicateEmail(exception);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(DuplicateUserEmailExceptionHandler.DUPLICATE_EMAIL_MESSAGE, response.getBody());
    }

    @Test
    void translatesTheConstraintNameFoundOnlyInTheDriverMessage() {
        // Some paths lose the Hibernate wrapper and carry the name in the message alone.
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "ERROR: duplicate key value violates unique constraint \"uk_users_email\""
        );

        assertEquals(HttpStatus.CONFLICT, handler.handleDuplicateEmail(exception).getStatusCode());
    }

    @Test
    void rethrowsEveryOtherIntegrityFailure() {
        // The point of keying on the constraint name: a foreign-key or not-null failure anywhere in
        // the API must not be reported as "this address is already taken".
        DataIntegrityViolationException foreignKey = new DataIntegrityViolationException(
                "could not execute statement",
                new ConstraintViolationException(
                        "violates foreign key constraint",
                        new SQLException("foreign key"),
                        "fk_courses_user_id"
                )
        );

        DataIntegrityViolationException thrown = assertThrows(
                DataIntegrityViolationException.class,
                () -> handler.handleDuplicateEmail(foreignKey)
        );
        assertTrue(thrown.getMessage().contains("could not execute statement"));
    }

    @Test
    void rethrowsAnIntegrityFailureWithNoConstraintNameAtAll() {
        DataIntegrityViolationException notNull =
                new DataIntegrityViolationException("null value in column \"name\" violates not-null constraint");

        assertThrows(
                DataIntegrityViolationException.class,
                () -> handler.handleDuplicateEmail(notNull)
        );
    }
}
