-- Phase 3: drills. video_library_item is global per organisation (shared across every coach's
-- clients); drill_assignment is the join table that puts a library item in front of one player,
-- optionally tied back to the analysis it came from.

CREATE TABLE video_library_item (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id         UUID        NOT NULL REFERENCES organization (id),
    title          TEXT        NOT NULL,
    cue            TEXT,
    category       TEXT        NOT NULL,
    video_asset_id UUID        NOT NULL REFERENCES video_asset (id),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT video_library_item_category_check CHECK (
        category IN ('FOREHAND', 'BACKHAND', 'SERVE', 'MOVEMENT', 'FITNESS'))
);

CREATE INDEX video_library_item_org_idx ON video_library_item (org_id);

CREATE TRIGGER video_library_item_set_updated_at
    BEFORE UPDATE ON video_library_item
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE drill_assignment (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID        NOT NULL REFERENCES organization (id),
    player_id       UUID        NOT NULL REFERENCES player_profile (id) ON DELETE CASCADE,
    library_item_id UUID        NOT NULL REFERENCES video_library_item (id),
    -- Null would mean a drill assigned outside of any analysis; every write path today always
    -- sets it, but the column stays nullable per the original data-model sketch.
    analysis_id     UUID        REFERENCES analysis (id) ON DELETE CASCADE,
    coach_note      TEXT,
    sort_order      INT         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX drill_assignment_player_idx ON drill_assignment (player_id);
CREATE INDEX drill_assignment_analysis_idx ON drill_assignment (analysis_id, sort_order);

CREATE TRIGGER drill_assignment_set_updated_at
    BEFORE UPDATE ON drill_assignment
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
