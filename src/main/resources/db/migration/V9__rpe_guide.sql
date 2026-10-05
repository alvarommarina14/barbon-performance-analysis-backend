-- The RPE guide is org-wide, PDF-only content — the same shape as recording_tips minus the
-- video half, since there is nothing to demonstrate on camera here.
CREATE TABLE rpe_guide (
    id                 UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id             UUID        NOT NULL UNIQUE REFERENCES organization (id),
    pdf_object_key     TEXT,
    pdf_content_type   TEXT,
    updated_by_user_id UUID        REFERENCES app_user (id),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER rpe_guide_set_updated_at
    BEFORE UPDATE ON rpe_guide
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
