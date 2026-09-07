-- Player uploads register the video_asset first and create the video_submission only once the
-- upload has landed (asset READY). The asset carries who started it, so another player cannot
-- claim the upload, and so an asset that never gets a submission can be swept.

ALTER TABLE video_asset
    ADD COLUMN uploaded_by_player_id UUID REFERENCES player_profile (id) ON DELETE CASCADE;

CREATE INDEX video_asset_uploader_idx ON video_asset (uploaded_by_player_id);
