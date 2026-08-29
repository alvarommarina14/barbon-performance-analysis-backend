package com.barbonperformance.courtproof.auth.application;

import com.barbonperformance.courtproof.auth.domain.InvalidRefreshTokenException;
import com.barbonperformance.courtproof.auth.dto.AuthDtos.CoachSummary;
import com.barbonperformance.courtproof.auth.dto.AuthDtos.CurrentUserResponse;
import com.barbonperformance.courtproof.auth.dto.AuthDtos.LoginRequest;
import com.barbonperformance.courtproof.auth.dto.AuthDtos.LoginResponse;
import com.barbonperformance.courtproof.auth.dto.AuthDtos.PlayerSummary;
import com.barbonperformance.courtproof.auth.dto.AuthDtos.RegisterRequest;
import com.barbonperformance.courtproof.auth.dto.AuthDtos.TokenPairResponse;
import com.barbonperformance.courtproof.auth.security.JwtService;
import com.barbonperformance.courtproof.auth.security.SecurityProperties;
import com.barbonperformance.courtproof.organization.domain.Organization;
import com.barbonperformance.courtproof.organization.domain.Organizations;
import com.barbonperformance.courtproof.shared.domain.ConflictException;
import com.barbonperformance.courtproof.shared.domain.NotFoundException;
import com.barbonperformance.courtproof.user.domain.AppUser;
import com.barbonperformance.courtproof.user.domain.AppUsers;
import com.barbonperformance.courtproof.user.domain.CoachProfile;
import com.barbonperformance.courtproof.user.domain.CoachProfiles;
import com.barbonperformance.courtproof.user.domain.PlayerProfile;
import com.barbonperformance.courtproof.user.domain.PlayerProfiles;
import com.barbonperformance.courtproof.user.domain.UserRole;
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
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokens;
    private final SecurityProperties properties;

    public AuthService(AppUsers users, PlayerProfiles playerProfiles,
            CoachProfiles coachProfiles, Organizations organizations,
            PasswordEncoder passwordEncoder, JwtService jwtService,
            RefreshTokenService refreshTokens, SecurityProperties properties) {
        this.users = users;
        this.playerProfiles = playerProfiles;
        this.coachProfiles = coachProfiles;
        this.organizations = organizations;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokens = refreshTokens;
        this.properties = properties;
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String email = AppUser.normalizeEmail(request.email());
        String slug = properties.organizationSlug();

        if (users.emailIsTaken(slug, email)) {
            throw new ConflictException("That email is already registered");
        }

        Organization organization = organizations.findBySlug(slug)
                .orElseThrow(() -> new IllegalStateException("Organisation " + slug + " is missing"));

        AppUser user = users.save(new AppUser(
                organization,
                email,
                passwordEncoder.encode(request.password()),
                UserRole.PLAYER,
                request.name().trim(),
                request.timezone() == null || request.timezone().isBlank()
                        ? organizationDefaultTimezone()
                        : request.timezone()));

        playerProfiles.save(new PlayerProfile(organization, user));

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
                jwtService.issueAccessToken(rotation.user()),
                rotation.refreshToken(),
                jwtService.accessTokenTtl().toSeconds());
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

    private LoginResponse issueSession(AppUser user) {
        return new LoginResponse(
                jwtService.issueAccessToken(user),
                refreshTokens.issueNewFamily(user),
                jwtService.accessTokenTtl().toSeconds(),
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
