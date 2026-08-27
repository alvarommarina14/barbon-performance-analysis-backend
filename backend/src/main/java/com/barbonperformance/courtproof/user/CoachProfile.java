package com.barbonperformance.courtproof.user;

import com.barbonperformance.courtproof.organization.Organization;
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

/**
 * Booking is not built: {@code bookingUrl} points at Cal.com, embedded the same way the
 * mockup does it.
 */
@Entity
@Table(name = "coach_profile")
public class CoachProfile {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(name = "booking_url")
    private String bookingUrl;

    @Column
    private String bio;

    protected CoachProfile() {
        // for JPA
    }

    public CoachProfile(Organization organization, AppUser user) {
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

    public String getBookingUrl() {
        return bookingUrl;
    }

    public void setBookingUrl(String bookingUrl) {
        this.bookingUrl = bookingUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}
