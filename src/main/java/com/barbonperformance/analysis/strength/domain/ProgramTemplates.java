package com.barbonperformance.analysis.strength.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgramTemplates {

    List<ProgramTemplate> findAllByOrgId(UUID orgId);

    Optional<ProgramTemplate> findById(UUID templateId);

    boolean existsByOrgIdAndName(UUID orgId, String name);

    ProgramTemplate save(ProgramTemplate template);

    void delete(ProgramTemplate template);
}
