-- Analysis always covers every stroke now, so the player no longer chooses one at submission
-- time. The CHECK constraint is dropped along with the column.
ALTER TABLE video_submission DROP COLUMN stroke_type;
