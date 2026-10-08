-- Sets and reps are prescribed per exercise and week now, so a cycle week no longer carries its own.
ALTER TABLE cycle_phase DROP COLUMN sets_reps;
