package com.barbonperformance.courtproof.video.infrastructure.web;

import com.barbonperformance.courtproof.shared.infrastructure.web.ProblemDetails;
import com.barbonperformance.courtproof.video.domain.InvalidWebhookSignatureException;
import com.barbonperformance.courtproof.video.domain.StreamUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class VideoExceptionHandler {

    @ExceptionHandler(InvalidWebhookSignatureException.class)
    ProblemDetail handleInvalidSignature(InvalidWebhookSignatureException e) {
        return ProblemDetails.of(HttpStatus.FORBIDDEN, "Invalid signature",
                "The webhook signature did not verify", "invalid-webhook-signature");
    }

    @ExceptionHandler(StreamUnavailableException.class)
    ProblemDetail handleStreamUnavailable(StreamUnavailableException e) {
        return ProblemDetails.of(HttpStatus.BAD_GATEWAY, "Video service unavailable",
                "The video service did not answer. Try again in a moment.", "stream-unavailable");
    }
}
