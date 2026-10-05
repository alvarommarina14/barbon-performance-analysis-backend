package com.barbonperformance.analysis.support;

import com.barbonperformance.analysis.drills.domain.DrillCategory;
import com.barbonperformance.analysis.drills.domain.LibraryItem;
import com.barbonperformance.analysis.drills.domain.LibraryItems;
import com.barbonperformance.analysis.organization.domain.Organization;
import com.barbonperformance.analysis.organization.domain.Organizations;
import com.barbonperformance.analysis.user.domain.AppUser;
import com.barbonperformance.analysis.user.domain.AppUsers;
import com.barbonperformance.analysis.user.domain.CoachProfile;
import com.barbonperformance.analysis.user.domain.CoachProfiles;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import com.barbonperformance.analysis.user.domain.PlayerProfiles;
import com.barbonperformance.analysis.user.domain.UserRole;
import com.barbonperformance.analysis.video.domain.VideoAsset;
import com.barbonperformance.analysis.video.domain.VideoAssetKind;
import com.barbonperformance.analysis.video.domain.VideoAssets;
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
    private final VideoAssets videoAssets;
    private final LibraryItems libraryItems;

    public TestFixtures(Organizations organizations, AppUsers users,
            CoachProfiles coachProfiles, PlayerProfiles playerProfiles,
            PasswordEncoder passwordEncoder, VideoAssets videoAssets, LibraryItems libraryItems) {
        this.organizations = organizations;
        this.users = users;
        this.coachProfiles = coachProfiles;
        this.playerProfiles = playerProfiles;
        this.passwordEncoder = passwordEncoder;
        this.videoAssets = videoAssets;
        this.libraryItems = libraryItems;
    }

    @Transactional
    public LibraryItem createLibraryItem(String title) {
        VideoAsset asset = videoAssets.save(new VideoAsset(organization(), VideoAssetKind.LIBRARY_DRILL));
        return libraryItems.save(new LibraryItem(organization(), title, null, DrillCategory.FITNESS, asset));
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
