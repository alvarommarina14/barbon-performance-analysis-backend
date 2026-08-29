package com.barbonperformance.courtproof.user.infrastructure.persistence;

import com.barbonperformance.courtproof.user.domain.AppUser;
import com.barbonperformance.courtproof.user.domain.AppUsers;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaAppUsers implements AppUsers {

    private final AppUserJpaRepository jpa;

    JpaAppUsers(AppUserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<AppUser> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<AppUser> findByEmail(String organizationSlug, String email) {
        return jpa.findByOrganizationSlugAndEmail(organizationSlug, email);
    }

    @Override
    public boolean emailIsTaken(String organizationSlug, String email) {
        return jpa.existsByOrganizationSlugAndEmail(organizationSlug, email);
    }

    @Override
    public AppUser save(AppUser user) {
        return jpa.save(user);
    }
}
