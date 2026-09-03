package com.barbonperformance.analysis.video.domain;

public class InvalidWebhookSignatureException extends RuntimeException {

    public InvalidWebhookSignatureException(String message) {
        super(message);
    }
}
