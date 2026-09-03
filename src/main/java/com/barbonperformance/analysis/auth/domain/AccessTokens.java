package com.barbonperformance.analysis.auth.domain;

import com.barbonperformance.analysis.user.domain.AppUser;
import java.time.Duration;
import java.util.Optional;

public interface AccessTokens {

    String issue(AppUser user);

    Optional<AuthenticatedUser> verify(String token);

    Duration ttl();
}
