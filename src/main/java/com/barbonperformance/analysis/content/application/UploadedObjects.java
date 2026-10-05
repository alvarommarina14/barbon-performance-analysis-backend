package com.barbonperformance.analysis.content.application;

import com.barbonperformance.analysis.shared.domain.ConflictException;

final class UploadedObjects {

    private UploadedObjects() {
    }

    // Each upload intent hands out one deterministic key. Confirming any other key would let a
    // caller point the org's content at an object it was never meant to reference.
    static void requireKey(String confirmedKey, String expectedKey) {
        if (!expectedKey.equals(confirmedKey)) {
            throw new ConflictException("the uploaded object does not belong here");
        }
    }
}
