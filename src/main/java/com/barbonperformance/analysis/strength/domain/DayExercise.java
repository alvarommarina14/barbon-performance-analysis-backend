package com.barbonperformance.analysis.strength.domain;

import com.barbonperformance.analysis.drills.domain.LibraryItem;
import com.barbonperformance.analysis.organization.domain.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "day_exercise")
public class DayExercise {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "training_day_id", nullable = false)
    private TrainingDay trainingDay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "library_item_id", nullable = false)
    private LibraryItem libraryItem;

    @Column(name = "sets_reps_wk1")
    private String setsRepsWk1;

    @Column(name = "sets_reps_wk2")
    private String setsRepsWk2;

    @Column(name = "sets_reps_wk3")
    private String setsRepsWk3;

    @Column(name = "sets_reps_wk4")
    private String setsRepsWk4;

    @Column(name = "cue_override")
    private String cueOverride;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected DayExercise() {
        // for JPA
    }

    public DayExercise(Organization organization, TrainingDay trainingDay, LibraryItem libraryItem,
            String setsRepsWk1, String setsRepsWk2, String setsRepsWk3, String setsRepsWk4,
            String cueOverride, int sortOrder) {
        this.organization = organization;
        this.trainingDay = trainingDay;
        this.libraryItem = libraryItem;
        this.setsRepsWk1 = setsRepsWk1;
        this.setsRepsWk2 = setsRepsWk2;
        this.setsRepsWk3 = setsRepsWk3;
        this.setsRepsWk4 = setsRepsWk4;
        this.cueOverride = cueOverride;
        this.sortOrder = sortOrder;
    }

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public TrainingDay getTrainingDay() {
        return trainingDay;
    }

    public LibraryItem getLibraryItem() {
        return libraryItem;
    }

    public String getSetsRepsWk1() {
        return setsRepsWk1;
    }

    public String getSetsRepsWk2() {
        return setsRepsWk2;
    }

    public String getSetsRepsWk3() {
        return setsRepsWk3;
    }

    public String getSetsRepsWk4() {
        return setsRepsWk4;
    }

    public String getCueOverride() {
        return cueOverride;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
