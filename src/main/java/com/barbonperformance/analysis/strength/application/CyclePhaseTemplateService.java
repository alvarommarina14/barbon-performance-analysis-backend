package com.barbonperformance.analysis.strength.application;

import com.barbonperformance.analysis.shared.domain.ConflictException;
import com.barbonperformance.analysis.shared.domain.NotFoundException;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.CyclePhaseInput;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.CyclePhaseTemplateView;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.CyclePhaseView;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.ReplaceCyclePhaseTemplateRequest;
import com.barbonperformance.analysis.strength.domain.CyclePhase;
import com.barbonperformance.analysis.strength.domain.CyclePhases;
import com.barbonperformance.analysis.user.domain.CoachProfile;
import com.barbonperformance.analysis.user.domain.CoachProfiles;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CyclePhaseTemplateService {

    private static final Set<Integer> REQUIRED_WEEKS = Set.of(1, 2, 3, 4);

    private final CyclePhases cyclePhases;
    private final CoachProfiles coachProfiles;

    public CyclePhaseTemplateService(CyclePhases cyclePhases, CoachProfiles coachProfiles) {
        this.cyclePhases = cyclePhases;
        this.coachProfiles = coachProfiles;
    }

    @Transactional(readOnly = true)
    public List<CyclePhaseTemplateView> list(UUID coachUserId) {
        UUID orgId = coach(coachUserId).getOrganization().getId();
        Map<String, List<CyclePhase>> byName = cyclePhases.findAllTemplateRowsByOrgId(orgId).stream()
                .collect(Collectors.groupingBy(CyclePhase::getTemplateName, LinkedHashMap::new,
                        Collectors.toList()));
        return byName.entrySet().stream()
                .map(entry -> new CyclePhaseTemplateView(entry.getKey(), describeAll(entry.getValue())))
                .toList();
    }

    @Transactional
    public CyclePhaseTemplateView createOrReplace(UUID coachUserId, String name,
            ReplaceCyclePhaseTemplateRequest request) {
        CoachProfile coach = coach(coachUserId);
        String templateName = name.trim();
        if (templateName.isEmpty()) {
            throw new ConflictException("a template needs a name");
        }

        Set<Integer> weeks = request.weeks().stream().map(CyclePhaseInput::weekNumber)
                .collect(Collectors.toSet());
        if (!weeks.equals(REQUIRED_WEEKS)) {
            throw new ConflictException("a template needs exactly weeks 1 through 4, once each");
        }

        Map<Integer, CyclePhase> existingByWeek = cyclePhases
                .findAllByOrgIdAndTemplateName(coach.getOrganization().getId(), templateName).stream()
                .collect(Collectors.toMap(CyclePhase::getWeekNumber, week -> week));

        List<CyclePhase> toSave = new ArrayList<>();
        for (CyclePhaseInput input : request.weeks()) {
            CyclePhase week = existingByWeek.get(input.weekNumber());
            if (week == null) {
                week = CyclePhase.forTemplate(coach.getOrganization(), templateName, input.weekNumber(),
                        input.phaseName(), input.goal(), input.setsReps(), input.rpeRange());
            } else {
                week.updateDetails(input.phaseName(), input.goal(), input.setsReps(), input.rpeRange());
            }
            toSave.add(week);
        }

        return new CyclePhaseTemplateView(templateName, describeAll(cyclePhases.saveAll(toSave)));
    }

    @Transactional
    public void delete(UUID coachUserId, String name) {
        UUID orgId = coach(coachUserId).getOrganization().getId();
        if (!cyclePhases.existsByOrgIdAndTemplateName(orgId, name)) {
            throw new NotFoundException("Template \"" + name + "\" not found");
        }
        cyclePhases.deleteAllByOrgIdAndTemplateName(orgId, name);
    }

    private CoachProfile coach(UUID coachUserId) {
        return coachProfiles.findByUserId(coachUserId)
                .orElseThrow(() -> new NotFoundException("No coach profile for this account"));
    }

    private static List<CyclePhaseView> describeAll(List<CyclePhase> weeks) {
        return weeks.stream()
                .sorted(Comparator.comparingInt(CyclePhase::getWeekNumber))
                .map(CyclePhaseService::describe)
                .toList();
    }
}
