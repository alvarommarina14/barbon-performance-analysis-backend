package com.barbonperformance.analysis.strength.domain;

import com.barbonperformance.analysis.organization.domain.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "program_template")
public class ProgramTemplate {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String name;

    @Column
    private String about;

    @Column
    private String schedule;

    @Column(name = "cycle_note")
    private String cycleNote;

    protected ProgramTemplate() {
        // for JPA
    }

    public ProgramTemplate(Organization organization, String name, String about, String schedule, String cycleNote) {
        this.organization = organization;
        this.name = name;
        this.about = about;
        this.schedule = schedule;
        this.cycleNote = cycleNote;
    }

    public void revise(String name, String about, String schedule, String cycleNote) {
        this.name = name;
        this.about = about;
        this.schedule = schedule;
        this.cycleNote = cycleNote;
    }

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public String getName() {
        return name;
    }

    public String getAbout() {
        return about;
    }

    public String getSchedule() {
        return schedule;
    }

    public String getCycleNote() {
        return cycleNote;
    }
}
