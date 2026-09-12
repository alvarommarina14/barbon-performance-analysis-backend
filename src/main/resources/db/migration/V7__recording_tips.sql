-- The recording-tips video reuses the ordinary video pipeline (a video_asset with its own kind),
-- same as a drill; only the PDF needs a plain object, since it can never go through a streaming
-- provider.
ALTER TABLE video_asset DROP CONSTRAINT video_asset_kind_check;
ALTER TABLE video_asset ADD CONSTRAINT video_asset_kind_check CHECK (
    kind IN ('PLAYER_UPLOAD', 'COACH_FEEDBACK', 'LIBRARY_DRILL', 'PROTOCOL', 'RECORDING_TIPS'));

CREATE TABLE recording_tips (
    id                 UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id             UUID        NOT NULL UNIQUE REFERENCES organization (id),
    video_asset_id     UUID        REFERENCES video_asset (id),
    pdf_object_key     TEXT,
    pdf_content_type   TEXT,
    updated_by_user_id UUID        REFERENCES app_user (id),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER recording_tips_set_updated_at
    BEFORE UPDATE ON recording_tips
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
