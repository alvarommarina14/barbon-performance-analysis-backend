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
@Table(name = "day_section")
public class DaySection {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "training_day_id", nullable = false)
    private TrainingDay trainingDay;

    @Column(nullable = false)
    private String title;

    @Column
    private String note;

    @Column(name = "varies_by_week", nullable = false)
    private boolean variesByWeek;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected DaySection() {
        // for JPA
    }

    public DaySection(TrainingDay trainingDay, String title, String note, boolean variesByWeek, int sortOrder) {
        this.organization = trainingDay.getOrganization();
        this.trainingDay = trainingDay;
        this.title = title;
        this.note = note;
        this.variesByWeek = variesByWeek;
        this.sortOrder = sortOrder;
    }

    public void revise(String title, String note, boolean variesByWeek, int sortOrder) {
        this.title = title;
        this.note = note;
        this.variesByWeek = variesByWeek;
        this.sortOrder = sortOrder;
    }

    public UUID getId() {
        return id;
    }

    public TrainingDay getTrainingDay() {
        return trainingDay;
    }

    public String getTitle() {
        return title;
    }

    public String getNote() {
        return note;
    }

    public boolean variesByWeek() {
        return variesByWeek;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
