package com.barbonperformance.analysis.video.domain;

// The player asked to create a submission before the video finished uploading. Retryable: the
// client polls until the upload lands.
public class VideoNotReadyException extends RuntimeException {

    public VideoNotReadyException(String message) {
        super(message);
    }
}
