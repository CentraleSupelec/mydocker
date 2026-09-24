package fr.centralesupelec.thuv.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * A login from a provider we do not operate resolved to a privileged account. Still a 403 for
 * callers that let it propagate; its own type so that a caller which must answer differently,
 * the LTI launch, can catch this refusal and no other, without matching on message text.
 */
public class PrivilegedAccountRefusedException extends ResponseStatusException {
    public PrivilegedAccountRefusedException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
