package com.barbonperformance.analysis.user.domain;

import com.barbonperformance.analysis.organization.domain.Organization;
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

    @Column(name = "plan_started_at")
    private LocalDate planStartedAt;

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

    public LocalDate getPlanStartedAt() {
        return planStartedAt;
    }

    public void setPlanStartedAt(LocalDate planStartedAt) {
        this.planStartedAt = planStartedAt;
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
