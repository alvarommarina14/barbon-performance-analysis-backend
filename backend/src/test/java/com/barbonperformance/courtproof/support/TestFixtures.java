package com.barbonperformance.courtproof.support;

import com.barbonperformance.courtproof.organization.Organization;
import com.barbonperformance.courtproof.organization.OrganizationRepository;
import com.barbonperformance.courtproof.user.AppUser;
import com.barbonperformance.courtproof.user.AppUserRepository;
import com.barbonperformance.courtproof.user.CoachProfile;
import com.barbonperformance.courtproof.user.CoachProfileRepository;
import com.barbonperformance.courtproof.user.PlayerProfile;
import com.barbonperformance.courtproof.user.PlayerProfileRepository;
import com.barbonperformance.courtproof.user.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coaches and admins have no public registration endpoint, so tests create them directly.
 */
@Component
public class TestFixtures {

    public static final String ORGANIZATION_SLUG = "barbon-performance";

    private final OrganizationRepository organizations;
    private final AppUserRepository users;
    private final CoachProfileRepository coachProfiles;
    private final PlayerProfileRepository playerProfiles;
    private final PasswordEncoder passwordEncoder;

    public TestFixtures(OrganizationRepository organizations, AppUserRepository users,
            CoachProfileRepository coachProfiles, PlayerProfileRepository playerProfiles,
            PasswordEncoder passwordEncoder) {
        this.organizations = organizations;
        this.users = users;
        this.coachProfiles = coachProfiles;
        this.playerProfiles = playerProfiles;
        this.passwordEncoder = passwordEncoder;
    }

    public Organization organization() {
        return organizations.findBySlug(ORGANIZATION_SLUG).orElseThrow();
    }

    @Transactional
    public CoachProfile createCoach(String email, String password, String name) {
        AppUser user = users.save(new AppUser(organization(), email, passwordEncoder.encode(password),
                UserRole.COACH, name, "America/Argentina/Buenos_Aires"));
        return coachProfiles.save(new CoachProfile(organization(), user));
    }

    @Transactional
    public PlayerProfile createPlayer(String email, String password, String name, CoachProfile coach) {
        AppUser user = users.save(new AppUser(organization(), email, passwordEncoder.encode(password),
                UserRole.PLAYER, name, "America/Argentina/Buenos_Aires"));
        PlayerProfile profile = new PlayerProfile(organization(), user);
        profile.setCoach(coach);
        return playerProfiles.save(profile);
    }
}
