package com.barbonperformance.courtproof.common;

import com.barbonperformance.courtproof.auth.InvalidRefreshTokenException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Errors are returned as RFC 7807 problem details, which springdoc puts in the OpenAPI spec
 * and both clients can render without guessing at a shape.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String BASE_TYPE = "https://courtproof.app/problems/";

    /**
     * Field-level validation failures. The clients render these next to the input that
     * caused them, so the field names have to survive into the response.
     */
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

    /**
     * Deliberately vague: telling a caller whether the email exists turns the login form into
     * an account enumeration oracle.
     */
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
