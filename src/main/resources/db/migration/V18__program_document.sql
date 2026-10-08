-- What the generated program PDF needs beyond the plan the app already holds, and templates that
-- carry a whole program instead of only the four cycle weeks.
--
-- A program is the same shape whether it belongs to a player or to a named template: cycle weeks,
-- training days, named blocks inside a day, exercises inside a block. training_day and cycle_phase
-- therefore belong to exactly one of a player or a program_template. Applying a template to a
-- player is a one-time copy, never a live link.

CREATE TABLE program_template (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id     UUID        NOT NULL REFERENCES organization (id),
    name       TEXT        NOT NULL,
    about      TEXT,
    schedule   TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT program_template_name_key UNIQUE (org_id, name)
);

CREATE TRIGGER program_template_set_updated_at
    BEFORE UPDATE ON program_template
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- The player's own cover for the document: level, "about your program", how to schedule the days.
CREATE TABLE training_program (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id        UUID        NOT NULL REFERENCES organization (id),
    player_id     UUID        NOT NULL UNIQUE REFERENCES player_profile (id) ON DELETE CASCADE,
    athlete_level TEXT,
    about         TEXT,
    schedule      TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER training_program_set_updated_at
    BEFORE UPDATE ON training_program
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Cycle-phase templates become program templates that so far hold only their four weeks.
INSERT INTO program_template (org_id, name)
SELECT DISTINCT org_id, template_name FROM cycle_phase WHERE template_name IS NOT NULL;

ALTER TABLE cycle_phase ADD COLUMN template_id UUID REFERENCES program_template (id) ON DELETE CASCADE;

UPDATE cycle_phase AS phase
SET template_id = template.id
FROM program_template AS template
WHERE template.org_id = phase.org_id AND template.name = phase.template_name;

DROP INDEX cycle_phase_template_week_key;
ALTER TABLE cycle_phase DROP CONSTRAINT cycle_phase_owner_check;
ALTER TABLE cycle_phase DROP COLUMN template_name;
ALTER TABLE cycle_phase ADD CONSTRAINT cycle_phase_owner_check
    CHECK ((player_id IS NULL) <> (template_id IS NULL));

CREATE UNIQUE INDEX cycle_phase_template_week_key
    ON cycle_phase (template_id, week_number) WHERE template_id IS NOT NULL;

ALTER TABLE training_day ALTER COLUMN player_id DROP NOT NULL;
ALTER TABLE training_day ADD COLUMN template_id UUID REFERENCES program_template (id) ON DELETE CASCADE;
ALTER TABLE training_day ADD CONSTRAINT training_day_owner_check
    CHECK ((player_id IS NULL) <> (template_id IS NULL));

CREATE INDEX training_day_template_idx ON training_day (template_id, sort_order) WHERE template_id IS NOT NULL;

-- A cardio day is described by a protocol text instead of a list of exercises.
ALTER TABLE training_day DROP CONSTRAINT training_day_kind_check;
ALTER TABLE training_day ADD CONSTRAINT training_day_kind_check
    CHECK (kind IN ('REHAB', 'STRENGTH', 'CARDIO'));
ALTER TABLE training_day ADD COLUMN protocol TEXT;

CREATE TABLE day_section (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID        NOT NULL REFERENCES organization (id),
    training_day_id UUID        NOT NULL REFERENCES training_day (id) ON DELETE CASCADE,
    title           TEXT        NOT NULL,
    note            TEXT,
    sort_order      INT         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX day_section_training_day_idx ON day_section (training_day_id, sort_order);

CREATE TRIGGER day_section_set_updated_at
    BEFORE UPDATE ON day_section
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Every existing exercise list becomes a single "Main work" block, so an exercise always has one.
INSERT INTO day_section (org_id, training_day_id, title, sort_order)
SELECT DISTINCT day.org_id, day.id, 'Main work', 0
FROM training_day AS day
JOIN day_exercise AS exercise ON exercise.training_day_id = day.id;

ALTER TABLE day_exercise ADD COLUMN section_id UUID REFERENCES day_section (id) ON DELETE CASCADE;
ALTER TABLE day_exercise ADD COLUMN notes TEXT;

UPDATE day_exercise AS exercise
SET section_id = section.id
FROM day_section AS section
WHERE section.training_day_id = exercise.training_day_id;

ALTER TABLE day_exercise ALTER COLUMN section_id SET NOT NULL;

CREATE INDEX day_exercise_section_idx ON day_exercise (section_id, sort_order);
