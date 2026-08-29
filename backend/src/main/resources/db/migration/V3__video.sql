-- Phase 2: the video loop.
--
-- Video bytes never pass through the backend. A video_asset is the local record of something
-- that lives in Cloudflare Stream, and it is created before Cloudflare knows about it: the
-- upload intent needs a submission id to put in the asset's metadata, so the rows come first
-- and stream_uid is filled in once Cloudflare accepts the direct-upload request.

CREATE TABLE video_asset (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id           UUID        NOT NULL REFERENCES organization (id),
    stream_uid       TEXT,
    playback_id      TEXT,
    kind             TEXT        NOT NULL,
    status           TEXT        NOT NULL DEFAULT 'PENDING_UPLOAD',
    duration_seconds INT,
    thumbnail_url    TEXT,
    size_bytes       BIGINT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT video_asset_kind_check CHECK (
        kind IN ('PLAYER_UPLOAD', 'COACH_FEEDBACK', 'LIBRARY_DRILL', 'PROTOCOL')),
    CONSTRAINT video_asset_status_check CHECK (
        status IN ('PENDING_UPLOAD', 'UPLOADING', 'PROCESSING', 'READY', 'ERRORED')),
    -- Postgres allows many NULLs under a unique constraint, which is what lets the row exist
    -- before Cloudflare has issued a uid.
    CONSTRAINT video_asset_stream_uid_key UNIQUE (stream_uid)
);

CREATE TRIGGER video_asset_set_updated_at
    BEFORE UPDATE ON video_asset
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE video_submission (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id         UUID        NOT NULL REFERENCES organization (id),
    player_id      UUID        NOT NULL REFERENCES player_profile (id) ON DELETE CASCADE,
    video_asset_id UUID        NOT NULL REFERENCES video_asset (id),
    stroke_type    TEXT        NOT NULL,
    player_note    TEXT,
    recorded_at    TIMESTAMPTZ,
    status         TEXT        NOT NULL DEFAULT 'SUBMITTED',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- The drill categories the mockup filters by, plus an escape hatch.
    CONSTRAINT video_submission_stroke_check CHECK (
        stroke_type IN ('FOREHAND', 'BACKHAND', 'SERVE', 'MOVEMENT', 'OTHER')),
    CONSTRAINT video_submission_status_check CHECK (
        status IN ('SUBMITTED', 'IN_REVIEW', 'ANALYZED')),
    CONSTRAINT video_submission_asset_key UNIQUE (video_asset_id)
);

CREATE INDEX video_submission_player_idx ON video_submission (player_id);
CREATE INDEX video_submission_status_idx ON video_submission (status);

CREATE TRIGGER video_submission_set_updated_at
    BEFORE UPDATE ON video_submission
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE analysis (
    id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id                  UUID        NOT NULL REFERENCES organization (id),
    -- Null when the coach filmed the player themselves rather than working from a submission.
    submission_id           UUID        REFERENCES video_submission (id) ON DELETE SET NULL,
    player_id               UUID        NOT NULL REFERENCES player_profile (id) ON DELETE CASCADE,
    coach_id                UUID        NOT NULL REFERENCES coach_profile (id),
    title                   TEXT        NOT NULL,
    summary                 TEXT,
    feedback_video_asset_id UUID        REFERENCES video_asset (id),
    status                  TEXT        NOT NULL DEFAULT 'DRAFT',
    published_at            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT analysis_status_check CHECK (status IN ('DRAFT', 'PUBLISHED')),
    -- A published analysis is one the player can watch, so it must have a video.
    CONSTRAINT analysis_published_has_video_check CHECK (
        status <> 'PUBLISHED' OR (feedback_video_asset_id IS NOT NULL AND published_at IS NOT NULL))
);

CREATE INDEX analysis_player_idx ON analysis (player_id, status);
CREATE INDEX analysis_coach_idx ON analysis (coach_id, status);

CREATE TRIGGER analysis_set_updated_at
    BEFORE UPDATE ON analysis
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- The 0:42 / 4:15 / 9:30 marks in the mockup. Stored as rows with a timestamp rather than
-- burned into the MP4, which is what makes them clickable in the player.
CREATE TABLE analysis_finding (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id            UUID        NOT NULL REFERENCES organization (id),
    analysis_id       UUID        NOT NULL REFERENCES analysis (id) ON DELETE CASCADE,
    timestamp_seconds INT         NOT NULL,
    title             TEXT        NOT NULL,
    detail            TEXT,
    sort_order        INT         NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT analysis_finding_timestamp_check CHECK (timestamp_seconds >= 0)
);

CREATE INDEX analysis_finding_analysis_idx ON analysis_finding (analysis_id, sort_order);

CREATE TRIGGER analysis_finding_set_updated_at
    BEFORE UPDATE ON analysis_finding
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
