-- One note per player per plan week. The coach can write all twelve up front or one a week; the
-- player only ever receives the weeks they have reached.
CREATE TABLE weekly_note (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id     UUID        NOT NULL REFERENCES organization (id),
    player_id  UUID        NOT NULL REFERENCES player_profile (id) ON DELETE CASCADE,
    plan_week  INT         NOT NULL,
    body       TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT weekly_note_plan_week_check CHECK (plan_week BETWEEN 1 AND 12),
    CONSTRAINT weekly_note_player_week_key UNIQUE (player_id, plan_week)
);

CREATE TRIGGER weekly_note_set_updated_at
    BEFORE UPDATE ON weekly_note
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
