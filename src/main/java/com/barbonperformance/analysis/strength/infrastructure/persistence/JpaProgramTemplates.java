package com.barbonperformance.analysis.strength.infrastructure.persistence;

import com.barbonperformance.analysis.strength.domain.ProgramTemplate;
import com.barbonperformance.analysis.strength.domain.ProgramTemplates;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaProgramTemplates implements ProgramTemplates {

    private final ProgramTemplateJpaRepository jpa;

    JpaProgramTemplates(ProgramTemplateJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<ProgramTemplate> findAllByOrgId(UUID orgId) {
        return jpa.findAllByOrganizationIdOrderByName(orgId);
    }

    @Override
    public Optional<ProgramTemplate> findById(UUID templateId) {
        return jpa.findById(templateId);
    }

    @Override
    public boolean existsByOrgIdAndName(UUID orgId, String name) {
        return jpa.existsByOrganizationIdAndName(orgId, name);
    }

    @Override
    public ProgramTemplate save(ProgramTemplate template) {
        return jpa.save(template);
    }

    @Override
    public void delete(ProgramTemplate template) {
        jpa.delete(template);
    }
}
