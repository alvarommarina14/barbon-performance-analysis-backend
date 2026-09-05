package com.barbonperformance.analysis.auth.application;

import com.barbonperformance.analysis.auth.SecurityProperties;
import com.barbonperformance.analysis.auth.application.dto.AuthDtos.CoachSummary;
import com.barbonperformance.analysis.auth.application.dto.AuthDtos.CurrentUserResponse;
import com.barbonperformance.analysis.auth.application.dto.AuthDtos.LoginRequest;
import com.barbonperformance.analysis.auth.application.dto.AuthDtos.LoginResponse;
import com.barbonperformance.analysis.auth.application.dto.AuthDtos.PlayerSummary;
import com.barbonperformance.analysis.auth.application.dto.AuthDtos.RegisterCoachRequest;
import com.barbonperformance.analysis.auth.application.dto.AuthDtos.RegisterRequest;
import com.barbonperformance.analysis.auth.application.dto.AuthDtos.TokenPairResponse;
import com.barbonperformance.analysis.auth.domain.AccessTokens;
import com.barbonperformance.analysis.auth.domain.InvalidRefreshTokenException;
import com.barbonperformance.analysis.organization.domain.Organization;
import com.barbonperformance.analysis.organization.domain.Organizations;
import com.barbonperformance.analysis.shared.domain.ConflictException;
import com.barbonperformance.analysis.shared.domain.NotFoundException;
import com.barbonperformance.analysis.user.domain.AppUser;
import com.barbonperformance.analysis.user.domain.AppUsers;
import com.barbonperformance.analysis.user.domain.CoachProfile;
import com.barbonperformance.analysis.user.domain.CoachProfiles;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import com.barbonperformance.analysis.user.domain.PlayerProfiles;
import com.barbonperformance.analysis.user.domain.UserRole;
import java.util.UUID;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUsers users;
    private final PlayerProfiles playerProfiles;
    private final CoachProfiles coachProfiles;
    private final Organizations organizations;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokens accessTokens;
    private final RefreshTokenService refreshTokens;
    private final SecurityProperties properties;

    public AuthService(AppUsers users, PlayerProfiles playerProfiles,
            CoachProfiles coachProfiles, Organizations organizations,
            PasswordEncoder passwordEncoder, AccessTokens accessTokens,
            RefreshTokenService refreshTokens, SecurityProperties properties) {
        this.users = users;
        this.playerProfiles = playerProfiles;
        this.coachProfiles = coachProfiles;
        this.organizations = organizations;
        this.passwordEncoder = passwordEncoder;
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
        this.properties = properties;
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        Organization organization = organizationForNewAccount(request.email());

        AppUser user = users.save(newUser(organization, request.email(), request.password(),
                UserRole.PLAYER, request.name(), request.timezone()));

        PlayerProfile profile = new PlayerProfile(organization, user);
        coachProfiles.findByOrganizationId(organization.getId()).ifPresent(profile::setCoach);
        playerProfiles.save(profile);

        return issueSession(user);
    }

    @Transactional
    public LoginResponse registerCoach(RegisterCoachRequest request) {
        Organization organization = organizationForNewAccount(request.email());

        // Phase 1: the organisation has exactly one coach, and this open endpoint bootstraps it.
        // Later coaches arrive through an invite flow, and this guard is what gets removed then.
        // See docs/adr/0002-coach-onboarding.md.
        if (coachProfiles.findByOrganizationId(organization.getId()).isPresent()) {
            throw new ConflictException("This organisation already has a coach");
        }

        AppUser user = users.save(newUser(organization, request.email(), request.password(),
                UserRole.COACH, request.name(), request.timezone()));
        coachProfiles.save(new CoachProfile(organization, user));

        return issueSession(user);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        AppUser user = users
                .findByEmail(
                        properties.organizationSlug(), AppUser.normalizeEmail(request.email()))
                .orElseThrow(() -> new BadCredentialsException("Unknown email"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Wrong password");
        }

        return issueSession(user);
    }

    // Must not roll back either, or the outer transaction undoes the family revocation
    // that RefreshTokenService performs when it detects a reused token.
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public TokenPairResponse refresh(String rawRefreshToken) {
        RefreshTokenService.Rotation rotation = refreshTokens.rotate(rawRefreshToken);
        return new TokenPairResponse(
                accessTokens.issue(rotation.user()),
                rotation.refreshToken(),
                accessTokens.ttl().toSeconds());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokens.revoke(rawRefreshToken);
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(UUID userId) {
        AppUser user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));
        return describe(user);
    }

    private Organization organizationForNewAccount(String email) {
        String slug = properties.organizationSlug();
        if (users.emailIsTaken(slug, AppUser.normalizeEmail(email))) {
            throw new ConflictException("That email is already registered");
        }
        return organizations.findBySlug(slug)
                .orElseThrow(() -> new IllegalStateException("Organisation " + slug + " is missing"));
    }

    private AppUser newUser(Organization organization, String email, String rawPassword,
            UserRole role, String name, String timezone) {
        return new AppUser(
                organization,
                email,
                passwordEncoder.encode(rawPassword),
                role,
                name.trim(),
                timezone == null || timezone.isBlank() ? organizationDefaultTimezone() : timezone);
    }

    private LoginResponse issueSession(AppUser user) {
        return new LoginResponse(
                accessTokens.issue(user),
                refreshTokens.issueNewFamily(user),
                accessTokens.ttl().toSeconds(),
                describe(user));
    }

    private CurrentUserResponse describe(AppUser user) {
        PlayerSummary player = null;
        CoachSummary coach = null;

        if (user.getRole() == UserRole.PLAYER) {
            player = playerProfiles.findByUserId(user.getId())
                    .map(profile -> new PlayerSummary(
                            profile.getId(),
                            profile.getMembershipTier(),
                            profile.getCurrentWeek(),
                            profile.getCoach() != null))
                    .orElse(null);
        } else if (user.getRole() == UserRole.COACH) {
            coach = coachProfiles.findByUserId(user.getId())
                    .map(profile -> new CoachSummary(profile.getId(), profile.getBookingUrl()))
                    .orElse(null);
        }

        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getAvatarUrl(),
                user.getTimezone(),
                player,
                coach);
    }

    private String organizationDefaultTimezone() {
        return "America/Argentina/Buenos_Aires";
    }
}
