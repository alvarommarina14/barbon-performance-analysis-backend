package com.barbonperformance.courtproof.auth.controller;

import com.barbonperformance.courtproof.auth.domain.InvalidRefreshTokenException;
import com.barbonperformance.courtproof.shared.controller.ProblemDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

    // Deliberately vague: telling a caller whether the email exists turns the login form into
    // an account enumeration oracle.
    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail handleBadCredentials(BadCredentialsException e) {
        return ProblemDetails.of(HttpStatus.UNAUTHORIZED, "Invalid credentials",
                "Email or password is incorrect", "invalid-credentials");
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    ProblemDetail handleInvalidRefreshToken(InvalidRefreshTokenException e) {
        return ProblemDetails.of(HttpStatus.UNAUTHORIZED, "Session expired",
                "Log in again", "invalid-refresh-token");
    }
}
