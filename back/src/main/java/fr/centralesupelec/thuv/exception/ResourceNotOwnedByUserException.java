package fr.centralesupelec.thuv.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ResourceNotOwnedByUserException extends Exception {
    public ResourceNotOwnedByUserException(String message) {
        super(message);
    }
}