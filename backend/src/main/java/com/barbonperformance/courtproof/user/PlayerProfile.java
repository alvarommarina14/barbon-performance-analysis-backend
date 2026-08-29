package com.barbonperformance.courtproof.user;

import com.barbonperformance.courtproof.organization.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "player_profile")
public class PlayerProfile {

    public static final int CYCLE_WEEKS = 4;

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coach_id")
    private CoachProfile coach;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_tier", nullable = false)
    private MembershipTier membershipTier = MembershipTier.BASE;

    @Column(name = "current_week", nullable = false)
    private int currentWeek = 1;

    @Column(name = "cycle_started_at")
    private LocalDate cycleStartedAt;

    @Column(name = "program_pdf_key")
    private String programPdfKey;

    @Column(name = "next_call_at")
    private Instant nextCallAt;

    @Column(name = "injury_notes")
    private String injuryNotes;

    protected PlayerProfile() {
        // for JPA
    }

    public PlayerProfile(Organization organization, AppUser user) {
        this.organization = organization;
        this.user = user;
    }

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public AppUser getUser() {
        return user;
    }

    public CoachProfile getCoach() {
        return coach;
    }

    public void setCoach(CoachProfile coach) {
        this.coach = coach;
    }

    public MembershipTier getMembershipTier() {
        return membershipTier;
    }

    public void setMembershipTier(MembershipTier membershipTier) {
        this.membershipTier = membershipTier;
    }

    public int getCurrentWeek() {
        return currentWeek;
    }

    public void setCurrentWeek(int currentWeek) {
        if (currentWeek < 1 || currentWeek > CYCLE_WEEKS) {
            throw new IllegalArgumentException("current week must be between 1 and " + CYCLE_WEEKS);
        }
        this.currentWeek = currentWeek;
    }

    public LocalDate getCycleStartedAt() {
        return cycleStartedAt;
    }

    public void setCycleStartedAt(LocalDate cycleStartedAt) {
        this.cycleStartedAt = cycleStartedAt;
    }

    public String getProgramPdfKey() {
        return programPdfKey;
    }

    public void setProgramPdfKey(String programPdfKey) {
        this.programPdfKey = programPdfKey;
    }

    public Instant getNextCallAt() {
        return nextCallAt;
    }

    public void setNextCallAt(Instant nextCallAt) {
        this.nextCallAt = nextCallAt;
    }

    public String getInjuryNotes() {
        return injuryNotes;
    }

    public void setInjuryNotes(String injuryNotes) {
        this.injuryNotes = injuryNotes;
    }
}
