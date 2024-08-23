package fr.centralesupelec.thuv.exception;

public class OIDCAuthenticationException extends RuntimeException {
    public OIDCAuthenticationException(String message) {
        super(message);
    }
}
