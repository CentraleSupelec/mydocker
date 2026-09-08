package fr.centralesupelec.thuv.security.controllers;

import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

/**
 * Translates a violation of the unique constraint on users.email into HTTP 409.
 *
 * <p>Keyed on the constraint name, not on the exception type. A blanket mapping of
 * DataIntegrityViolationException would turn every foreign-key and not-null failure in the
 * application into "this address is already taken", which is why
 * {@link AuthenticationExceptionHandler} refuses to do it. Anything that is not this constraint is
 * rethrown, so unrelated integrity failures keep the behaviour they had before the constraint
 * existed.
 *
 * <p>This covers the admin write paths, {@code POST /user} and {@code PUT /user/{id}}, which had no
 * conflict handling at all: before the constraint they could not collide on an address, and after it
 * they would have returned 500.
 */
@RestControllerAdvice
public class DuplicateUserEmailExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DuplicateUserEmailExceptionHandler.class);

    static final String CONSTRAINT_NAME = "uk_users_email";

    static final String DUPLICATE_EMAIL_MESSAGE =
            "Another MyDocker account already uses this email address. "
                    + "Addresses are compared without regard to upper and lower case.";

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDuplicateEmail(DataIntegrityViolationException ex) {
        if (!violatesEmailConstraint(ex)) {
            throw ex;
        }
        LOGGER.warn("Rejected a write that would have duplicated an email address: {}", CONSTRAINT_NAME);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(DUPLICATE_EMAIL_MESSAGE);
    }

    /**
     * Walks the cause chain rather than reading the top-level message. Hibernate reports the
     * constraint name on its own exception type, but the driver's message is the only carrier on
     * some paths, so both are checked.
     */
    private boolean violatesEmailConstraint(Throwable throwable) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && CONSTRAINT_NAME.equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }
            String message = cause.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains(CONSTRAINT_NAME)) {
                return true;
            }
            if (cause.getCause() == cause) {
                return false;
            }
        }
        return false;
    }
}
