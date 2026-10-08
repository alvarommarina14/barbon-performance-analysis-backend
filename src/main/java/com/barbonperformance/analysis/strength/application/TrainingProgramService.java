package com.barbonperformance.analysis.strength.application;

import com.barbonperformance.analysis.strength.application.dto.ProgramDtos.TrainingProgramView;
import com.barbonperformance.analysis.strength.application.dto.ProgramDtos.WriteTrainingProgramRequest;
import com.barbonperformance.analysis.strength.domain.TrainingProgram;
import com.barbonperformance.analysis.strength.domain.TrainingPrograms;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainingProgramService {

    private final TrainingPrograms programs;
    private final PlayerAccess access;

    public TrainingProgramService(TrainingPrograms programs, PlayerAccess access) {
        this.programs = programs;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public TrainingProgramView viewForCaller(UUID userId, UUID playerProfileId) {
        PlayerProfile player = access.readableBy(userId, playerProfileId);
        return programs.findByPlayerId(player.getId())
                .map(TrainingProgramService::describe)
                .orElse(new TrainingProgramView(null, null, null, null));
    }

    @Transactional
    public TrainingProgramView write(UUID coachUserId, UUID playerProfileId, WriteTrainingProgramRequest request) {
        PlayerProfile player = access.ownedByCoach(coachUserId, playerProfileId);
        TrainingProgram program = programOf(player);
        program.revise(blankToNull(request.athleteLevel()), blankToNull(request.about()),
                blankToNull(request.schedule()), blankToNull(request.cycleNote()));
        return describe(programs.save(program));
    }

    TrainingProgram programOf(PlayerProfile player) {
        return programs.findByPlayerId(player.getId()).orElseGet(() -> new TrainingProgram(player));
    }

    TrainingProgram save(TrainingProgram program) {
        return programs.save(program);
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static TrainingProgramView describe(TrainingProgram program) {
        return new TrainingProgramView(program.getAthleteLevel(), program.getAbout(), program.getSchedule(),
                program.getCycleNote());
    }
}
