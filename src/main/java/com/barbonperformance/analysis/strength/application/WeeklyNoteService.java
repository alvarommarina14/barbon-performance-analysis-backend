package com.barbonperformance.analysis.strength.application;

import com.barbonperformance.analysis.shared.domain.NotFoundException;
import com.barbonperformance.analysis.strength.application.dto.WeeklyNoteDtos.WeeklyNoteView;
import com.barbonperformance.analysis.strength.application.dto.WeeklyNoteDtos.WriteWeeklyNoteRequest;
import com.barbonperformance.analysis.strength.domain.WeeklyNote;
import com.barbonperformance.analysis.strength.domain.WeeklyNotes;
import com.barbonperformance.analysis.user.domain.PlanCalendar;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import com.barbonperformance.analysis.user.domain.UserRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WeeklyNoteService {

    private final WeeklyNotes notes;
    private final PlayerAccess access;

    public WeeklyNoteService(WeeklyNotes notes, PlayerAccess access) {
        this.notes = notes;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<WeeklyNoteView> listForCaller(UUID userId, UUID playerProfileId) {
        PlayerProfile player = access.readableBy(userId, playerProfileId);
        int visibleWeeks = access.role(userId) == UserRole.PLAYER
                ? PlanCalendar.unlockedWeeks(player)
                : PlanCalendar.PLAN_WEEKS;
        return describeAll(player, visibleWeeks);
    }

    // An empty body deletes the week's note, so the coach clears a note the same way they edit it.
    @Transactional
    public List<WeeklyNoteView> write(UUID coachUserId, UUID playerProfileId, int planWeek,
            WriteWeeklyNoteRequest request) {
        if (planWeek < 1 || planWeek > PlanCalendar.PLAN_WEEKS) {
            throw new NotFoundException("Plan week " + planWeek + " does not exist");
        }
        PlayerProfile player = access.ownedByCoach(coachUserId, playerProfileId);
        String body = request.body() == null ? "" : request.body().strip();
        Optional<WeeklyNote> existing = notes.findByPlayerIdAndPlanWeek(player.getId(), planWeek);

        if (body.isEmpty()) {
            existing.ifPresent(notes::delete);
        } else {
            WeeklyNote note = existing.orElseGet(() -> new WeeklyNote(player, planWeek, body));
            note.rewrite(body);
            notes.save(note);
        }
        return describeAll(player, PlanCalendar.PLAN_WEEKS);
    }

    private List<WeeklyNoteView> describeAll(PlayerProfile player, int visibleWeeks) {
        return notes.findAllByPlayerId(player.getId()).stream()
                .filter(note -> note.getPlanWeek() <= visibleWeeks)
                .map(note -> new WeeklyNoteView(
                        note.getPlanWeek(),
                        note.getBody(),
                        player.getPlanStartedAt() == null ? null
                                : PlanCalendar.weekStart(player.getPlanStartedAt(), note.getPlanWeek()),
                        note.getUpdatedAt()))
                .toList();
    }
}
