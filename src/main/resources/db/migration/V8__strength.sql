-- Phase 4: strength and conditioning. cycle_phase rows belong to exactly one of a player or a
-- named org-level template (never both) — a template is just a reusable, complete cycle_phase
-- that a coach can copy into a player's own rows instead of retyping one from scratch; applying
-- one is a one-time copy, not a live link. training_day and day_exercise are per-player, and
-- day_exercise reuses video_library_item for its video instead of a second video pipeline.

CREATE TABLE cycle_phase (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id        UUID        NOT NULL REFERENCES organization (id),
    player_id     UUID        REFERENCES player_profile (id) ON DELETE CASCADE,
    template_name TEXT,
    week_number   INT         NOT NULL,
    phase_name    TEXT        NOT NULL,
    goal          TEXT        NOT NULL,
    sets_reps     TEXT,
    rpe_range     TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT cycle_phase_week_number_check CHECK (week_number BETWEEN 1 AND 4),
    CONSTRAINT cycle_phase_owner_check CHECK (
        (player_id IS NOT NULL AND template_name IS NULL) OR
        (player_id IS NULL AND template_name IS NOT NULL)
    )
);

CREATE UNIQUE INDEX cycle_phase_player_week_key
    ON cycle_phase (player_id, week_number) WHERE player_id IS NOT NULL;

CREATE UNIQUE INDEX cycle_phase_template_week_key
    ON cycle_phase (org_id, template_name, week_number) WHERE template_name IS NOT NULL;

CREATE TRIGGER cycle_phase_set_updated_at
    BEFORE UPDATE ON cycle_phase
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE training_day (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id           UUID        NOT NULL REFERENCES organization (id),
    player_id        UUID        NOT NULL REFERENCES player_profile (id) ON DELETE CASCADE,
    day_label        TEXT        NOT NULL,
    title            TEXT        NOT NULL,
    focus            TEXT,
    duration_minutes INT,
    kind             TEXT        NOT NULL,
    sort_order       INT         NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT training_day_kind_check CHECK (kind IN ('REHAB', 'STRENGTH'))
);

CREATE INDEX training_day_player_idx ON training_day (player_id, sort_order);

CREATE TRIGGER training_day_set_updated_at
    BEFORE UPDATE ON training_day
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE day_exercise (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID        NOT NULL REFERENCES organization (id),
    training_day_id UUID        NOT NULL REFERENCES training_day (id) ON DELETE CASCADE,
    library_item_id UUID        NOT NULL REFERENCES video_library_item (id),
    sets_reps_wk1   TEXT,
    sets_reps_wk2   TEXT,
    sets_reps_wk3   TEXT,
    sets_reps_wk4   TEXT,
    cue_override    TEXT,
    sort_order      INT         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX day_exercise_training_day_idx ON day_exercise (training_day_id, sort_order);

CREATE TRIGGER day_exercise_set_updated_at
    BEFORE UPDATE ON day_exercise
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
