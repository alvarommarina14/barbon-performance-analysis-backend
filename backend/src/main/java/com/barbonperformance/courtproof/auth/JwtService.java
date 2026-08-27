package com.barbonperformance.courtproof.auth;

import com.barbonperformance.courtproof.config.SecurityProperties;
import com.barbonperformance.courtproof.user.AppUser;
import com.barbonperformance.courtproof.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies the short-lived access token. Refresh tokens are opaque and handled by
 * {@link RefreshTokenService} — they are deliberately not JWTs, because a JWT cannot be revoked.
 */
@Service
public class JwtService {

    static final String CLAIM_ROLE = "role";
    static final String CLAIM_EMAIL = "email";

    private final SecretKey key;
    private final Duration accessTokenTtl;

    public JwtService(SecurityProperties properties) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.jwtSecret()));
        this.accessTokenTtl = properties.accessTokenTtl();
    }

    public String issueAccessToken(AppUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_EMAIL, user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(key)
                .compact();
    }

    public Duration accessTokenTtl() {
        return accessTokenTtl;
    }

    /** Empty when the token is malformed, tampered with or expired. */
    public Optional<AuthenticatedUser> verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Optional.of(new AuthenticatedUser(
                    UUID.fromString(claims.getSubject()),
                    claims.get(CLAIM_EMAIL, String.class),
                    UserRole.valueOf(claims.get(CLAIM_ROLE, String.class))));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
