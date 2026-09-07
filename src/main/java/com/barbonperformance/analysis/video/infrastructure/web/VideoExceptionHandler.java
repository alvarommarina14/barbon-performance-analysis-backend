package com.barbonperformance.analysis.video.infrastructure.web;

import com.barbonperformance.analysis.shared.infrastructure.web.ProblemDetails;
import com.barbonperformance.analysis.video.domain.InvalidWebhookSignatureException;
import com.barbonperformance.analysis.video.domain.StreamUnavailableException;
import com.barbonperformance.analysis.video.domain.VideoNotReadyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class VideoExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(VideoExceptionHandler.class);

    @ExceptionHandler(InvalidWebhookSignatureException.class)
    ProblemDetail handleInvalidSignature(InvalidWebhookSignatureException e) {
        log.warn("Rejected a video webhook: {}", e.getMessage());
        return ProblemDetails.of(HttpStatus.FORBIDDEN, "Invalid signature",
                "The webhook signature did not verify", "invalid-webhook-signature");
    }

    @ExceptionHandler(VideoNotReadyException.class)
    ProblemDetail handleVideoNotReady(VideoNotReadyException e) {
        return ProblemDetails.of(HttpStatus.CONFLICT, "Video not ready",
                "The video is still uploading. Try again in a moment.", "video-not-ready");
    }

    @ExceptionHandler(StreamUnavailableException.class)
    ProblemDetail handleStreamUnavailable(StreamUnavailableException e) {
        // The client only sees a generic 502, so the real cause has to reach the log.
        log.error("Video provider call failed: {}", e.getMessage());
        return ProblemDetails.of(HttpStatus.BAD_GATEWAY, "Video service unavailable",
                "The video service did not answer. Try again in a moment.", "stream-unavailable");
    }
}
