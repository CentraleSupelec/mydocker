package fr.centralesupelec.thuv.security.controllers;

import fr.centralesupelec.thuv.exception.UserUpsertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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
     * Covers the login paths that do not catch this themselves: LTI, which has no try/catch at all,
     * and any future caller. CAS and magic link catch it locally because their own catch-all would
     * otherwise swallow it first.
     *
     * <p>DataIntegrityViolationException is handled here too: once the unique constraint on
     * users.email exists, an ambiguous login surfaces at insert time rather than at lookup time,
     * and the two failures mean the same thing to the person logging in.
     */
    @ExceptionHandler({UserUpsertException.class, DataIntegrityViolationException.class})
    public ResponseEntity<String> handleAmbiguousAccount(Exception ex) {
        LOGGER.error("Cannot resolve a single account for this login: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(AMBIGUOUS_ACCOUNT_MESSAGE);
    }
}
