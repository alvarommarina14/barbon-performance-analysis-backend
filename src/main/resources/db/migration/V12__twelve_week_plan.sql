-- A player's plan runs twelve weeks: the four-week cycle (Base, Volume, Intensity, Deload) three
-- times over. The week is derived from the start date, so current_week — never written by any
-- endpoint — goes away rather than becoming a second source of truth.
ALTER TABLE player_profile RENAME COLUMN cycle_started_at TO plan_started_at;
ALTER TABLE player_profile DROP CONSTRAINT player_profile_week_check;
ALTER TABLE player_profile DROP COLUMN current_week;
