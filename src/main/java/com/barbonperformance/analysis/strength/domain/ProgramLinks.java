package com.barbonperformance.analysis.strength.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

// Absolute links that work outside the app, with no bearer token: they are opened from a PDF or
// handed to the phone's browser. Each one is signed, so the id it carries cannot be swapped.
public interface ProgramLinks {

    // Never expires: a PDF the player keeps must keep working. The redirect behind it mints a
    // fresh short-lived playback URL on every open.
    String exerciseVideo(UUID libraryItemId);

    Optional<UUID> exerciseVideoItem(String token);

    String programPdf(UUID playerProfileId, Instant expiresAt);

    Optional<UUID> programPdfPlayer(String token, Instant now);
}
