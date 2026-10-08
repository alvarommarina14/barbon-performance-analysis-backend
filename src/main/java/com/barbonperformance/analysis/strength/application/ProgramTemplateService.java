package com.barbonperformance.analysis.strength.application;

import static com.barbonperformance.analysis.strength.application.TrainingProgramService.blankToNull;

import com.barbonperformance.analysis.shared.domain.ConflictException;
import com.barbonperformance.analysis.shared.domain.NotFoundException;
import com.barbonperformance.analysis.strength.application.dto.ProgramDtos.ApplyProgramTemplateRequest;
import com.barbonperformance.analysis.strength.application.dto.ProgramDtos.CreateProgramTemplateRequest;
import com.barbonperformance.analysis.strength.application.dto.ProgramDtos.ProgramTemplateSummary;
import com.barbonperformance.analysis.strength.application.dto.ProgramDtos.ProgramTemplateView;
import com.barbonperformance.analysis.strength.application.dto.ProgramDtos.UpdateProgramTemplateRequest;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.CyclePhaseInput;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.ReplaceCyclePhasesRequest;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.ReplaceTrainingDaysRequest;
import com.barbonperformance.analysis.strength.domain.CyclePhase;
import com.barbonperformance.analysis.strength.domain.CyclePhases;
import com.barbonperformance.analysis.strength.domain.ProgramTemplate;
import com.barbonperformance.analysis.strength.domain.ProgramTemplates;
import com.barbonperformance.analysis.strength.domain.TrainingDay;
import com.barbonperformance.analysis.strength.domain.TrainingDays;
import com.barbonperformance.analysis.strength.domain.TrainingProgram;
import com.barbonperformance.analysis.user.domain.CoachProfile;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// A template holds a whole program — cycle weeks, days, blocks, exercises, and the cover texts — in
// the same rows a player's program uses. Applying one to a player copies it once; editing either
// side afterwards never touches the other.
@Service
public class ProgramTemplateService {

    private final ProgramTemplates templates;
    private final TrainingDays trainingDays;
    private final CyclePhases cyclePhases;
    private final ProgramCopier copier;
    private final TrainingDayService trainingDayService;
    private final CyclePhaseService cyclePhaseService;
    private final TrainingProgramService trainingProgramService;
    private final PlayerAccess access;

    public ProgramTemplateService(ProgramTemplates templates, TrainingDays trainingDays, CyclePhases cyclePhases,
            ProgramCopier copier, TrainingDayService trainingDayService, CyclePhaseService cyclePhaseService,
            TrainingProgramService trainingProgramService, PlayerAccess access) {
        this.templates = templates;
        this.trainingDays = trainingDays;
        this.cyclePhases = cyclePhases;
        this.copier = copier;
        this.trainingDayService = trainingDayService;
        this.cyclePhaseService = cyclePhaseService;
        this.trainingProgramService = trainingProgramService;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<ProgramTemplateSummary> list(UUID coachUserId) {
        UUID orgId = access.coach(coachUserId).getOrganization().getId();
        return templates.findAllByOrgId(orgId).stream()
                .map(template -> new ProgramTemplateSummary(template.getId(), template.getName(),
                        trainingDays.findAllByTemplateId(template.getId()).size()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProgramTemplateView view(UUID coachUserId, UUID templateId) {
        return describe(load(coachUserId, templateId));
    }

    @Transactional
    public ProgramTemplateView create(UUID coachUserId, CreateProgramTemplateRequest request) {
        CoachProfile coach = access.coach(coachUserId);
        String name = requireFreeName(coach, request.name().strip(), null);

        if (request.fromPlayerProfileId() == null) {
            return describe(templates.save(new ProgramTemplate(coach.getOrganization(), name, null, null, null)));
        }

        PlayerProfile player = access.ownedByCoach(coachUserId, request.fromPlayerProfileId());
        TrainingProgram cover = trainingProgramService.programOf(player);
        ProgramTemplate template = templates.save(
                new ProgramTemplate(coach.getOrganization(), name, cover.getAbout(), cover.getSchedule(),
                        cover.getCycleNote()));
        copier.copyDays(trainingDays.findAllByPlayerId(player.getId()), day -> TrainingDay.forTemplate(template,
                day.getTitle(), day.getFocus(), day.getDurationMinutes(), day.getKind(), day.getProtocol(),
                day.getSortOrder()));
        cyclePhases.saveAll(cyclePhases.findAllByPlayerId(player.getId()).stream()
                .map(week -> CyclePhase.forTemplate(template, week.getWeekNumber(), week.getPhaseName(),
                        week.getGoal(), week.getRpeRange(), week.getExample()))
                .toList());
        return describe(template);
    }

    @Transactional
    public ProgramTemplateView update(UUID coachUserId, UUID templateId, UpdateProgramTemplateRequest request) {
        ProgramTemplate template = load(coachUserId, templateId);
        String name = requireFreeName(access.coach(coachUserId), request.name().strip(), template.getName());
        template.revise(name, blankToNull(request.about()), blankToNull(request.schedule()),
                blankToNull(request.cycleNote()));
        return describe(templates.save(template));
    }

    @Transactional
    public void delete(UUID coachUserId, UUID templateId) {
        templates.delete(load(coachUserId, templateId));
    }

    @Transactional
    public ProgramTemplateView replaceWeeks(UUID coachUserId, UUID templateId, ReplaceCyclePhasesRequest request) {
        ProgramTemplate template = load(coachUserId, templateId);
        CyclePhaseService.requireExactlyRequiredWeeks(request.phases().stream().map(CyclePhaseInput::weekNumber));

        Map<Integer, CyclePhase> existing = cyclePhases.findAllByTemplateId(template.getId()).stream()
                .collect(Collectors.toMap(CyclePhase::getWeekNumber, Function.identity()));
        List<CyclePhase> toSave = new ArrayList<>();
        for (CyclePhaseInput input : request.phases()) {
            CyclePhase week = existing.get(input.weekNumber());
            if (week == null) {
                week = CyclePhase.forTemplate(template, input.weekNumber(), input.phaseName(), input.goal(),
                        input.rpeRange(), input.example());
            } else {
                week.updateDetails(input.phaseName(), input.goal(), input.rpeRange(), input.example());
            }
            toSave.add(week);
        }
        cyclePhases.saveAll(toSave);
        return describe(template);
    }

    @Transactional
    public ProgramTemplateView replaceDays(UUID coachUserId, UUID templateId, ReplaceTrainingDaysRequest request) {
        ProgramTemplate template = load(coachUserId, templateId);
        trainingDayService.replaceDays(trainingDays.findAllByTemplateId(template.getId()), request.days(),
                (input, order) -> TrainingDay.forTemplate(template, input.title(), input.focus(),
                        input.durationMinutes(), input.kind(), TrainingDayService.protocolOf(input), order));
        return describe(template);
    }

    // Replaces the player's days (and with them the logs against those exercises) and cycle weeks.
    // The cover texts are taken from the template only where it has them; the level stays the player's.
    @Transactional
    public void applyToPlayer(UUID coachUserId, UUID playerProfileId, ApplyProgramTemplateRequest request) {
        PlayerProfile player = access.ownedByCoach(coachUserId, playerProfileId);
        ProgramTemplate template = load(coachUserId, request.templateId());

        trainingDays.deleteAll(trainingDays.findAllByPlayerId(player.getId()));
        copier.copyDays(trainingDays.findAllByTemplateId(template.getId()), day -> TrainingDay.forPlayer(player,
                day.getTitle(), day.getFocus(), day.getDurationMinutes(), day.getKind(), day.getProtocol(),
                day.getSortOrder()));

        cyclePhases.saveAll(cyclePhases.findAllByTemplateId(template.getId()).stream()
                .map(week -> cyclePhaseService.upsertPlayerWeek(player, week.getWeekNumber(), week.getPhaseName(),
                        week.getGoal(), week.getRpeRange(), week.getExample()))
                .toList());

        TrainingProgram cover = trainingProgramService.programOf(player);
        cover.revise(cover.getAthleteLevel(),
                template.getAbout() != null ? template.getAbout() : cover.getAbout(),
                template.getSchedule() != null ? template.getSchedule() : cover.getSchedule(),
                template.getCycleNote() != null ? template.getCycleNote() : cover.getCycleNote());
        trainingProgramService.save(cover);
    }

    private ProgramTemplate load(UUID coachUserId, UUID templateId) {
        UUID orgId = access.coach(coachUserId).getOrganization().getId();
        return templates.findById(templateId)
                .filter(template -> template.getOrganization().getId().equals(orgId))
                .orElseThrow(() -> new NotFoundException("Program template " + templateId + " not found"));
    }

    private String requireFreeName(CoachProfile coach, String name, String currentName) {
        if (!name.equals(currentName) && templates.existsByOrgIdAndName(coach.getOrganization().getId(), name)) {
            throw new ConflictException("a template named \"" + name + "\" already exists");
        }
        return name;
    }

    private ProgramTemplateView describe(ProgramTemplate template) {
        return new ProgramTemplateView(
                template.getId(),
                template.getName(),
                template.getAbout(),
                template.getSchedule(),
                template.getCycleNote(),
                cyclePhases.findAllByTemplateId(template.getId()).stream()
                        .map(CyclePhaseService::describe)
                        .toList(),
                trainingDayService.describeAll(trainingDays.findAllByTemplateId(template.getId())));
    }
}
