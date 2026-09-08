package fr.centralesupelec.thuv.security.controllers;

import fr.centralesupelec.thuv.exception.UserUpsertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthenticationExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationExceptionHandler.class);

    static final String AMBIGUOUS_ACCOUNT_MESSAGE =
            "Several MyDocker accounts share this email address, "
                    + "so we cannot tell which one to sign you in to. "
                    + "Please contact support: the accounts have to be merged.";

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<String> handleDisabledException() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Account disabled");
    }

    /**
     * Owns the translation for every login path. CAS and magic link rethrow the exception past
     * their own catch-all so that it arrives here; LTI has no try/catch at all and arrives here
     * directly.
     *
     * <p>Database integrity failures are deliberately not translated here. There is no unique
     * constraint on users.email yet, so an ambiguous login cannot surface at insert time, and a
     * blanket mapping of DataIntegrityViolationException would turn unrelated foreign-key and
     * not-null failures across the whole API into this message. When the constraint is added, its
     * violation gets translated in the same changeset that creates it, keyed on the constraint name.
     */
    @ExceptionHandler(UserUpsertException.class)
    public ResponseEntity<String> handleAmbiguousAccount(UserUpsertException ex) {
        LOGGER.error("Cannot resolve a single account for this login: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(AMBIGUOUS_ACCOUNT_MESSAGE);
    }
}
