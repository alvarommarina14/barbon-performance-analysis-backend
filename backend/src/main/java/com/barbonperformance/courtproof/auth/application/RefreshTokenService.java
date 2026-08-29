package com.barbonperformance.courtproof.auth.application;

import com.barbonperformance.courtproof.auth.domain.InvalidRefreshTokenException;
import com.barbonperformance.courtproof.auth.domain.RefreshToken;
import com.barbonperformance.courtproof.auth.domain.RefreshTokens;
import com.barbonperformance.courtproof.auth.security.SecurityProperties;
import com.barbonperformance.courtproof.user.domain.AppUser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokens refreshTokens;
    private final SecurityProperties properties;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokens refreshTokens, SecurityProperties properties) {
        this.refreshTokens = refreshTokens;
        this.properties = properties;
    }

    @Transactional
    public String issue(AppUser user, UUID familyId) {
        String raw = randomToken();
        RefreshToken token = new RefreshToken(
                user.getOrganization(),
                user,
                hash(raw),
                familyId,
                Instant.now().plus(properties.refreshTokenTtl()));
        refreshTokens.save(token);
        return raw;
    }

    @Transactional
    public String issueNewFamily(AppUser user) {
        return issue(user, UUID.randomUUID());
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public Rotation rotate(String rawToken) {
        Instant now = Instant.now();
        RefreshToken presented = refreshTokens.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidRefreshTokenException("Unknown refresh token"));

        if (presented.getRevokedAt() != null) {
            log.warn("Refresh token reuse detected for user {}; revoking family {}",
                    presented.getUser().getId(), presented.getFamilyId());
            refreshTokens.revokeFamily(presented.getFamilyId(), now);
            throw new InvalidRefreshTokenException("Refresh token has already been used");
        }

        if (!presented.isUsable(now)) {
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }

        presented.revoke(now);
        AppUser user = presented.getUser();
        return new Rotation(user, issue(user, presented.getFamilyId()));
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokens.findByTokenHash(hash(rawToken))
                .ifPresent(token -> token.revoke(Instant.now()));
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokens.revokeAllForUser(userId, Instant.now());
    }

    private String randomToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the JDK", e);
        }
    }

    public record Rotation(AppUser user, String refreshToken) {
    }
}
