-- The daily routine is one row per organisation: a warm-up video, a recovery video and one PDF
-- that covers both. It replaces the multi-row `protocol` table the architecture had sketched —
-- the product has exactly these three pieces, never a list. Both videos use the PROTOCOL asset
-- kind that video_asset already allows.
CREATE TABLE daily_routine (
    id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id                  UUID        NOT NULL UNIQUE REFERENCES organization (id),
    warmup_video_asset_id   UUID        REFERENCES video_asset (id),
    recovery_video_asset_id UUID        REFERENCES video_asset (id),
    pdf_object_key          TEXT,
    pdf_content_type        TEXT,
    pdf_file_name           TEXT,
    pdf_size_bytes          BIGINT,
    pdf_uploaded_at         TIMESTAMPTZ,
    updated_by_user_id      UUID        REFERENCES app_user (id),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER daily_routine_set_updated_at
    BEFORE UPDATE ON daily_routine
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
