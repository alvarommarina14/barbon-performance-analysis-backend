-- Warning signs the player reads before training. A row with no player_id applies to every client
-- of the organisation; a row with one is specific to that player. Both lists are always replaced
-- whole, so sort_order is simply the position the coach gave them.
CREATE TABLE red_flag (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id     UUID        NOT NULL REFERENCES organization (id),
    player_id  UUID        REFERENCES player_profile (id) ON DELETE CASCADE,
    title      TEXT        NOT NULL,
    body       TEXT,
    sort_order INT         NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX red_flag_org_generic_idx ON red_flag (org_id, sort_order) WHERE player_id IS NULL;
CREATE INDEX red_flag_player_idx ON red_flag (player_id, sort_order) WHERE player_id IS NOT NULL;

CREATE TRIGGER red_flag_set_updated_at
    BEFORE UPDATE ON red_flag
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
