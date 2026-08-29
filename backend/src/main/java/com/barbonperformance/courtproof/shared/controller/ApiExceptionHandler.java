package com.barbonperformance.courtproof.shared.controller;

import com.barbonperformance.courtproof.auth.domain.InvalidRefreshTokenException;
import com.barbonperformance.courtproof.shared.domain.ConflictException;
import com.barbonperformance.courtproof.shared.domain.NotFoundException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String BASE_TYPE = "https://courtproof.app/problems/";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed",
                "Some fields are invalid", "validation-failed");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Not found", e.getMessage(), "not-found");
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException e) {
        return problem(HttpStatus.CONFLICT, "Conflict", e.getMessage(), "conflict");
    }

    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail handleBadCredentials(BadCredentialsException e) {
        return problem(HttpStatus.UNAUTHORIZED, "Invalid credentials",
                "Email or password is incorrect", "invalid-credentials");
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    ProblemDetail handleInvalidRefreshToken(InvalidRefreshTokenException e) {
        return problem(HttpStatus.UNAUTHORIZED, "Session expired",
                "Log in again", "invalid-refresh-token");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException e) {
        return problem(HttpStatus.FORBIDDEN, "Forbidden",
                "You do not have access to this resource", "forbidden");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, String slug) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(BASE_TYPE + slug));
        return problem;
    }
}
