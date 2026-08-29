package com.barbonperformance.courtproof.support;

import com.barbonperformance.courtproof.organization.domain.Organization;
import com.barbonperformance.courtproof.organization.persistence.OrganizationRepository;
import com.barbonperformance.courtproof.organization.persistence.OrganizationRepository;
import com.barbonperformance.courtproof.user.domain.AppUser;
import com.barbonperformance.courtproof.user.persistence.AppUserRepository;
import com.barbonperformance.courtproof.user.domain.CoachProfile;
import com.barbonperformance.courtproof.user.persistence.CoachProfileRepository;
import com.barbonperformance.courtproof.user.domain.PlayerProfile;
import com.barbonperformance.courtproof.user.persistence.PlayerProfileRepository;
import com.barbonperformance.courtproof.user.domain.UserRole;
import com.barbonperformance.courtproof.user.persistence.AppUserRepository;
import com.barbonperformance.courtproof.user.persistence.CoachProfileRepository;
import com.barbonperformance.courtproof.user.persistence.PlayerProfileRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
