package com.barbonperformance.analysis.strength.domain;

import com.barbonperformance.analysis.organization.domain.Organization;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "training_program")
public class TrainingProgram {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false, unique = true)
    private PlayerProfile player;

    @Column(name = "athlete_level")
    private String athleteLevel;

    @Column
    private String about;

    @Column
    private String schedule;

    @Column(name = "cycle_note")
    private String cycleNote;

    protected TrainingProgram() {
        // for JPA
    }

    public TrainingProgram(PlayerProfile player) {
        this.organization = player.getOrganization();
        this.player = player;
    }

    public void revise(String athleteLevel, String about, String schedule, String cycleNote) {
        this.athleteLevel = athleteLevel;
        this.about = about;
        this.schedule = schedule;
        this.cycleNote = cycleNote;
    }

    public UUID getId() {
        return id;
    }

    public PlayerProfile getPlayer() {
        return player;
    }

    public String getAthleteLevel() {
        return athleteLevel;
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
