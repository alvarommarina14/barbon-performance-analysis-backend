package com.barbonperformance.analysis.strength.application;

import com.barbonperformance.analysis.drills.domain.LibraryItem;
import com.barbonperformance.analysis.drills.domain.LibraryItems;
import com.barbonperformance.analysis.shared.domain.ConflictException;
import com.barbonperformance.analysis.shared.domain.NotFoundException;
import com.barbonperformance.analysis.strength.StrengthProperties;
import com.barbonperformance.analysis.strength.application.dto.ProgramDtos.ProgramPdfLinkView;
import com.barbonperformance.analysis.strength.application.dto.ProgramDtos.RenderedProgramPdf;
import com.barbonperformance.analysis.strength.domain.CyclePhases;
import com.barbonperformance.analysis.strength.domain.DayExercise;
import com.barbonperformance.analysis.strength.domain.DayExercises;
import com.barbonperformance.analysis.strength.domain.DaySection;
import com.barbonperformance.analysis.strength.domain.DaySections;
import com.barbonperformance.analysis.strength.domain.ProgramDocument;
import com.barbonperformance.analysis.strength.domain.ProgramDocumentRenderer;
import com.barbonperformance.analysis.strength.domain.ProgramLinks;
import com.barbonperformance.analysis.strength.domain.RedFlags;
import com.barbonperformance.analysis.strength.domain.TrainingDay;
import com.barbonperformance.analysis.strength.domain.TrainingDayKind;
import com.barbonperformance.analysis.strength.domain.TrainingDays;
import com.barbonperformance.analysis.strength.domain.TrainingProgram;
import com.barbonperformance.analysis.strength.domain.TrainingPrograms;
import com.barbonperformance.analysis.user.domain.CoachProfile;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import com.barbonperformance.analysis.user.domain.PlayerProfiles;
import com.barbonperformance.analysis.video.VideoProperties;
import com.barbonperformance.analysis.video.domain.StreamGateway;
import com.barbonperformance.analysis.video.domain.VideoAsset;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgramDocumentService {

    private final PlayerAccess access;
    private final PlayerProfiles playerProfiles;
    private final TrainingPrograms programs;
    private final TrainingDays trainingDays;
    private final DaySections daySections;
    private final DayExercises dayExercises;
    private final CyclePhases cyclePhases;
    private final RedFlags redFlags;
    private final LibraryItems libraryItems;
    private final StreamGateway stream;
    private final ProgramLinks links;
    private final ProgramDocumentRenderer renderer;
    private final StrengthProperties properties;
    private final VideoProperties videoProperties;

    public ProgramDocumentService(PlayerAccess access, PlayerProfiles playerProfiles, TrainingPrograms programs,
            TrainingDays trainingDays, DaySections daySections, DayExercises dayExercises, CyclePhases cyclePhases,
            RedFlags redFlags, LibraryItems libraryItems, StreamGateway stream, ProgramLinks links,
            ProgramDocumentRenderer renderer, StrengthProperties properties, VideoProperties videoProperties) {
        this.access = access;
        this.playerProfiles = playerProfiles;
        this.programs = programs;
        this.trainingDays = trainingDays;
        this.daySections = daySections;
        this.dayExercises = dayExercises;
        this.cyclePhases = cyclePhases;
        this.redFlags = redFlags;
        this.libraryItems = libraryItems;
        this.stream = stream;
        this.links = links;
        this.renderer = renderer;
        this.properties = properties;
        this.videoProperties = videoProperties;
    }

    @Transactional(readOnly = true)
    public ProgramPdfLinkView linkForCaller(UUID userId, UUID playerProfileId) {
        PlayerProfile player = access.readableBy(userId, playerProfileId);
        Instant expiresAt = Instant.now().plus(properties.programPdfLinkTtl());
        return new ProgramPdfLinkView(links.programPdf(player.getId(), expiresAt), expiresAt);
    }

    @Transactional(readOnly = true)
    public RenderedProgramPdf render(String token) {
        UUID playerProfileId = links.programPdfPlayer(token, Instant.now())
                .orElseThrow(() -> new NotFoundException("This program link is invalid or has expired"));
        PlayerProfile player = playerProfiles.findById(playerProfileId)
                .orElseThrow(() -> new NotFoundException("This program link is invalid or has expired"));
        String fileName = "Training_Program_" + player.getUser().getName().strip().replaceAll("[^\\p{L}\\p{N}]+", "_")
                + ".pdf";
        return new RenderedProgramPdf(fileName, renderer.render(document(player)));
    }

    @Transactional(readOnly = true)
    public String exerciseVideoUrl(String token) {
        UUID libraryItemId = links.exerciseVideoItem(token)
                .orElseThrow(() -> new NotFoundException("This video link is invalid"));
        LibraryItem item = libraryItems.findById(libraryItemId)
                .orElseThrow(() -> new NotFoundException("This exercise video is no longer available"));
        VideoAsset asset = item.getVideoAsset();
        if (!asset.isReady()) {
            throw new ConflictException("This exercise video is still processing");
        }
        return stream.signPlayback(asset.getStreamUid(), videoProperties.playbackTokenTtl()).watchUrl();
    }

    ProgramDocument document(PlayerProfile player) {
        TrainingProgram program = programs.findByPlayerId(player.getId()).orElse(null);
        CoachProfile coach = player.getCoach();
        return new ProgramDocument(
                player.getUser().getName(),
                program == null ? null : program.getAthleteLevel(),
                coach == null ? null : coach.getUser().getName(),
                player.getOrganization().getName(),
                coach == null ? null : coach.getUser().getEmail(),
                LocalDate.now(zoneOf(player)),
                program == null ? null : program.getAbout(),
                program == null ? null : program.getSchedule(),
                redFlags.findAllByPlayerId(player.getId()).stream()
                        .map(flag -> new ProgramDocument.RedFlagEntry(flag.getTitle(), flag.getBody()))
                        .toList(),
                cyclePhases.findAllByPlayerId(player.getId()).stream()
                        .map(phase -> new ProgramDocument.Week(phase.getWeekNumber(), phase.getPhaseName(),
                                phase.getGoal(), phase.getSetsReps(), phase.getRpeRange()))
                        .toList(),
                trainingDays.findAllByPlayerId(player.getId()).stream()
                        .map(this::describe)
                        .toList());
    }

    private ProgramDocument.Day describe(TrainingDay day) {
        Map<UUID, List<DayExercise>> bySection = dayExercises.findAllByTrainingDayId(day.getId()).stream()
                .collect(Collectors.groupingBy(exercise -> exercise.getSection().getId()));
        List<ProgramDocument.Block> blocks = day.getKind() == TrainingDayKind.CARDIO ? List.of()
                : daySections.findAllByTrainingDayId(day.getId()).stream()
                        .map(section -> describe(section, bySection.getOrDefault(section.getId(), List.of())))
                        .filter(block -> !block.exercises().isEmpty())
                        .toList();
        return new ProgramDocument.Day(day.getDayLabel(), day.getTitle(), day.getFocus(), day.getDurationMinutes(),
                day.getKind(), day.getProtocol(), blocks);
    }

    private ProgramDocument.Block describe(DaySection section, List<DayExercise> exercises) {
        return new ProgramDocument.Block(section.getTitle(), section.getNote(), exercises.stream()
                .sorted(Comparator.comparingInt(DayExercise::getSortOrder))
                .map(this::describe)
                .toList());
    }

    private ProgramDocument.Exercise describe(DayExercise exercise) {
        LibraryItem item = exercise.getLibraryItem();
        String cue = exercise.getCueOverride() != null ? exercise.getCueOverride() : item.getCue();
        String videoUrl = item.getVideoAsset().isReady() ? links.exerciseVideo(item.getId()) : null;
        return new ProgramDocument.Exercise(item.getTitle(), cue, exercise.getNotes(),
                exercise.setsRepsByWeek(), videoUrl);
    }

    private static ZoneId zoneOf(PlayerProfile player) {
        String timezone = player.getUser().getTimezone();
        if (timezone == null) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException e) {
            return ZoneOffset.UTC;
        }
    }
}
