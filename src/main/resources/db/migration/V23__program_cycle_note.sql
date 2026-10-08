-- The "4-week cycle" text of the PDF, separate from "how to schedule" the days.
ALTER TABLE training_program ADD COLUMN cycle_note TEXT;
ALTER TABLE program_template ADD COLUMN cycle_note TEXT;
