package com.barbonperformance.courtproof.video.domain;

import java.util.Optional;

public interface StreamWebhooks {

    boolean signatureIsValid(String rawBody, String signatureHeader);

    // Empty when the payload is a well-formed delivery this application has nothing to do with,
    // which is not an error: Cloudflare sends states we do not model.
    Optional<StreamEvent> parse(String rawBody);
}
