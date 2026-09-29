-- Same file-detail columns V10 added to recording_tips's PDF, mirrored here for the RPE guide.
ALTER TABLE rpe_guide ADD COLUMN pdf_file_name TEXT;
ALTER TABLE rpe_guide ADD COLUMN pdf_size_bytes BIGINT;
ALTER TABLE rpe_guide ADD COLUMN pdf_uploaded_at TIMESTAMPTZ;
