package com.barbonperformance.courtproof.auth.domain;

import com.barbonperformance.courtproof.user.domain.AppUser;
import java.time.Duration;
import java.util.Optional;

public interface AccessTokens {

    String issue(AppUser user);

    Optional<AuthenticatedUser> verify(String token);

    Duration ttl();
}
