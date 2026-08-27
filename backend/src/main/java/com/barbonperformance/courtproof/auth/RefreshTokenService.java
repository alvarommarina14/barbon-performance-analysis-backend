package com.barbonperformance.courtproof.auth;

import com.barbonperformance.courtproof.config.SecurityProperties;
import com.barbonperformance.courtproof.user.AppUser;
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

/**
 * Rotating refresh tokens.
 *
 * <p>A token is a 256-bit random string; only its SHA-256 hash is stored, so a dump of the
 * table hands over no live sessions. Every refresh revokes the token presented and issues a
 * new one in the same family.
 *
 * <p>Presenting an already-revoked token means either a replay or a stolen token being used
 * alongside the legitimate client. There is no way to tell which, so the whole family is
 * revoked and the user has to log in again. That is the intended trade-off.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokens;
    private final SecurityProperties properties;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokens, SecurityProperties properties) {
        this.refreshTokens = refreshTokens;
        this.properties = properties;
    }

    /** The raw token is returned once and never stored. */
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

    /** Starts a new family. Used at login. */
    @Transactional
    public String issueNewFamily(AppUser user) {
        return issue(user, UUID.randomUUID());
    }

    /**
     * Swaps a valid refresh token for a fresh one.
     *
     * <p>{@code noRollbackFor} is load-bearing: reuse detection revokes the family and then
     * throws, and a rollback would quietly undo exactly the revocation that makes the
     * detection worth having. The other failure paths write nothing, so committing is a no-op.
     *
     * @throws InvalidRefreshTokenException when the token is unknown, expired or already used
     */
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
