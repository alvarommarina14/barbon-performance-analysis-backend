-- The coach's recording-tips screen shows each file's original name, size and upload date. The
-- video already carries size/duration/created_at on video_asset; only its original file name is
-- missing there. The PDF has no backing entity at all, so it needs its own three columns.
ALTER TABLE video_asset ADD COLUMN original_file_name TEXT;

ALTER TABLE recording_tips ADD COLUMN pdf_file_name TEXT;
ALTER TABLE recording_tips ADD COLUMN pdf_size_bytes BIGINT;
ALTER TABLE recording_tips ADD COLUMN pdf_uploaded_at TIMESTAMPTZ;
