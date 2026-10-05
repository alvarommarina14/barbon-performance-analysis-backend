-- What the player actually did: one log per exercise per plan week, holding the sets they ran.
-- A null weight is a bodyweight set. Logs hang off day_exercise, which the coach now edits in
-- place, so reworking a program keeps the history of every exercise that survives the edit.
CREATE TABLE exercise_log (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID        NOT NULL REFERENCES organization (id),
    player_id       UUID        NOT NULL REFERENCES player_profile (id) ON DELETE CASCADE,
    day_exercise_id UUID        NOT NULL REFERENCES day_exercise (id) ON DELETE CASCADE,
    plan_week       INT         NOT NULL,
    unit            TEXT        NOT NULL,
    logged_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT exercise_log_plan_week_check CHECK (plan_week BETWEEN 1 AND 12),
    CONSTRAINT exercise_log_unit_check CHECK (unit IN ('LB', 'KG')),
    CONSTRAINT exercise_log_player_exercise_week_key UNIQUE (player_id, day_exercise_id, plan_week)
);

CREATE INDEX exercise_log_day_exercise_idx ON exercise_log (day_exercise_id, plan_week);

CREATE TABLE exercise_log_set (
    log_id     UUID          NOT NULL REFERENCES exercise_log (id) ON DELETE CASCADE,
    set_number INT           NOT NULL,
    reps       INT           NOT NULL,
    weight     NUMERIC(6, 2),
    PRIMARY KEY (log_id, set_number),
    CONSTRAINT exercise_log_set_reps_check CHECK (reps >= 0),
    CONSTRAINT exercise_log_set_weight_check CHECK (weight IS NULL OR weight >= 0)
);
