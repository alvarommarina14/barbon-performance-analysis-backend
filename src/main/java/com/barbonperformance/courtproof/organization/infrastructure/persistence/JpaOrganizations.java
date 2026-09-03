package com.barbonperformance.courtproof.organization.infrastructure.persistence;

import com.barbonperformance.courtproof.organization.domain.Organization;
import com.barbonperformance.courtproof.organization.domain.Organizations;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaOrganizations implements Organizations {

    private final OrganizationJpaRepository jpa;

    JpaOrganizations(OrganizationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Organization> findBySlug(String slug) {
        return jpa.findBySlug(slug);
    }
}
