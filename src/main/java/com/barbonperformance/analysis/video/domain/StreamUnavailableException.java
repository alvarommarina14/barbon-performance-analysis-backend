package com.barbonperformance.analysis.video.domain;

public class StreamUnavailableException extends RuntimeException {

    public StreamUnavailableException(String message) {
        super(message);
    }
}
