package fr.centralesupelec.thuv.security.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthenticationExceptionHandler {

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<String> handleDisabledException() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Account disabled");
    }
}
