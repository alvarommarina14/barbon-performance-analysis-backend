package com.barbonperformance.analysis.strength.infrastructure.security;

import com.barbonperformance.analysis.strength.StrengthProperties;
import com.barbonperformance.analysis.strength.domain.ProgramLinks;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
class HmacProgramLinks implements ProgramLinks {

    private static final String ALGORITHM = "HmacSHA256";
    // 128 bits of the MAC keeps the links short enough to print; still far beyond guessing.
    private static final int SIGNATURE_BYTES = 16;
    private static final String VIDEO = "video";
    private static final String PDF = "pdf";

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final StrengthProperties properties;
    private final SecretKeySpec key;

    HmacProgramLinks(StrengthProperties properties) {
        this.properties = properties;
        this.key = new SecretKeySpec(properties.linkSecret().getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    @Override
    public String exerciseVideo(UUID libraryItemId) {
        return properties.publicBaseUrl() + "/api/v1/public/exercise-videos/" + sign(VIDEO + ":" + libraryItemId);
    }

    @Override
    public Optional<UUID> exerciseVideoItem(String token) {
        return verify(token)
                .map(payload -> payload.split(":"))
                .filter(parts -> parts.length == 2 && VIDEO.equals(parts[0]))
                .flatMap(parts -> parseUuid(parts[1]));
    }

    @Override
    public String programPdf(UUID playerProfileId, Instant expiresAt) {
        return properties.publicBaseUrl() + "/api/v1/public/program-pdf/"
                + sign(PDF + ":" + playerProfileId + ":" + expiresAt.getEpochSecond());
    }

    @Override
    public Optional<UUID> programPdfPlayer(String token, Instant now) {
        return verify(token)
                .map(payload -> payload.split(":"))
                .filter(parts -> parts.length == 3 && PDF.equals(parts[0]))
                .filter(parts -> notExpired(parts[2], now))
                .flatMap(parts -> parseUuid(parts[1]));
    }

    private String sign(String payload) {
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        return ENCODER.encodeToString(bytes) + "." + ENCODER.encodeToString(mac(bytes));
    }

    private Optional<String> verify(String token) {
        int dot = token.indexOf('.');
        if (dot < 1) {
            return Optional.empty();
        }
        try {
            byte[] payload = DECODER.decode(token.substring(0, dot));
            byte[] signature = DECODER.decode(token.substring(dot + 1));
            if (!MessageDigest.isEqual(mac(payload), signature)) {
                return Optional.empty();
            }
            return Optional.of(new String(payload, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private byte[] mac(byte[] payload) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return Arrays.copyOf(mac.doFinal(payload), SIGNATURE_BYTES);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", e);
        }
    }

    private static boolean notExpired(String epochSeconds, Instant now) {
        try {
            return Instant.ofEpochSecond(Long.parseLong(epochSeconds)).isAfter(now);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
