-- A training day's label is now derived from its position ("Day 1", "Day 2", ...) instead of typed
-- by the coach. Renumber the existing days the same way, closing any gaps in sort_order.
UPDATE training_day AS day
SET sort_order = numbered.position - 1,
    day_label  = 'Day ' || numbered.position
FROM (
    SELECT id, row_number() OVER (PARTITION BY player_id ORDER BY sort_order, created_at) AS position
    FROM training_day
) AS numbered
WHERE day.id = numbered.id;
