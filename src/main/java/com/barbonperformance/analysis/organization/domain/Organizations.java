package com.barbonperformance.analysis.organization.domain;

import java.util.Optional;

public interface Organizations {

    Optional<Organization> findBySlug(String slug);
}
