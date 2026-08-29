package com.barbonperformance.courtproof.user.domain;

import java.util.Optional;
import java.util.UUID;

public interface AppUsers {

    Optional<AppUser> findById(UUID id);

    Optional<AppUser> findByEmail(String organizationSlug, String email);

    boolean emailIsTaken(String organizationSlug, String email);

    AppUser save(AppUser user);
}
