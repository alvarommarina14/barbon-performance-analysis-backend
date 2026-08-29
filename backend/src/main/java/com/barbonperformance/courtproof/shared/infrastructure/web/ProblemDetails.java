package com.barbonperformance.courtproof.shared.infrastructure.web;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class ProblemDetails {

    private static final String BASE_TYPE = "https://courtproof.app/problems/";

    private ProblemDetails() {
    }

    public static ProblemDetail of(HttpStatus status, String title, String detail, String slug) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(BASE_TYPE + slug));
        return problem;
    }
}
