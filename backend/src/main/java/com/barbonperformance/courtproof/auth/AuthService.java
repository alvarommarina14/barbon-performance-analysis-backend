package com.barbonperformance.courtproof.auth;

import com.barbonperformance.courtproof.auth.dto.AuthDtos.CoachSummary;
import com.barbonperformance.courtproof.auth.dto.AuthDtos.CurrentUserResponse;
import com.barbonperformance.courtproof.auth.dto.AuthDtos.LoginRequest;
import com.barbonperformance.courtproof.auth.dto.AuthDtos.LoginResponse;
import com.barbonperformance.courtproof.auth.dto.AuthDtos.PlayerSummary;
import com.barbonperformance.courtproof.auth.dto.AuthDtos.RegisterRequest;
import com.barbonperformance.courtproof.auth.dto.AuthDtos.TokenPairResponse;
import com.barbonperformance.courtproof.common.ConflictException;
import com.barbonperformance.courtproof.common.NotFoundException;
import com.barbonperformance.courtproof.config.SecurityProperties;
import com.barbonperformance.courtproof.organization.Organization;
import com.barbonperformance.courtproof.organization.OrganizationRepository;
import com.barbonperformance.courtproof.user.AppUser;
import com.barbonperformance.courtproof.user.AppUserRepository;
import com.barbonperformance.courtproof.user.CoachProfile;
import com.barbonperformance.courtproof.user.CoachProfileRepository;
import com.barbonperformance.courtproof.user.PlayerProfile;
import com.barbonperformance.courtproof.user.PlayerProfileRepository;
import com.barbonperformance.courtproof.user.UserRole;
import java.util.UUID;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository users;
    private final PlayerProfileRepository playerProfiles;
    private final CoachProfileRepository coachProfiles;
    private final OrganizationRepository organizations;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokens;
    private final SecurityProperties properties;

    public AuthService(AppUserRepository users, PlayerProfileRepository playerProfiles,
            CoachProfileRepository coachProfiles, OrganizationRepository organizations,
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

        if (users.existsByOrganizationSlugAndEmail(slug, email)) {
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
                .findByOrganizationSlugAndEmail(
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
