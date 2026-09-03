package com.barbonperformance.courtproof.support;

import com.barbonperformance.courtproof.organization.domain.Organization;
import com.barbonperformance.courtproof.organization.domain.Organizations;
import com.barbonperformance.courtproof.user.domain.AppUser;
import com.barbonperformance.courtproof.user.domain.AppUsers;
import com.barbonperformance.courtproof.user.domain.CoachProfile;
import com.barbonperformance.courtproof.user.domain.CoachProfiles;
import com.barbonperformance.courtproof.user.domain.PlayerProfile;
import com.barbonperformance.courtproof.user.domain.PlayerProfiles;
import com.barbonperformance.courtproof.user.domain.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TestFixtures {

    public static final String ORGANIZATION_SLUG = "barbon-performance";

    private final Organizations organizations;
    private final AppUsers users;
    private final CoachProfiles coachProfiles;
    private final PlayerProfiles playerProfiles;
    private final PasswordEncoder passwordEncoder;

    public TestFixtures(Organizations organizations, AppUsers users,
            CoachProfiles coachProfiles, PlayerProfiles playerProfiles,
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
