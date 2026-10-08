-- The RPE table's effort label ("Light/Moderate") and "reps left in the tank" for each cycle week.
ALTER TABLE cycle_phase ADD COLUMN effort TEXT;
ALTER TABLE cycle_phase ADD COLUMN reps_left TEXT;
