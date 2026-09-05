package com.barbonperformance.analysis.analysis.infrastructure.web;

import com.barbonperformance.analysis.analysis.domain.AnalysisNotEditableException;
import com.barbonperformance.analysis.analysis.domain.FeedbackVideoNotReadyException;
import com.barbonperformance.analysis.shared.infrastructure.web.ProblemDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AnalysisExceptionHandler {

    @ExceptionHandler(AnalysisNotEditableException.class)
    ProblemDetail handleNotEditable(AnalysisNotEditableException e) {
        return ProblemDetails.of(HttpStatus.CONFLICT, "Analysis not editable",
                e.getMessage(), "analysis-not-editable");
    }

    @ExceptionHandler(FeedbackVideoNotReadyException.class)
    ProblemDetail handleFeedbackNotReady(FeedbackVideoNotReadyException e) {
        return ProblemDetails.of(HttpStatus.CONFLICT, "Feedback video not ready",
                e.getMessage(), "feedback-video-not-ready");
    }
}
