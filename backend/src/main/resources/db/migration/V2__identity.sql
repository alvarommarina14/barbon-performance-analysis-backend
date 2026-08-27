-- Identity: users, the two profile shapes, and rotating refresh tokens.

CREATE TABLE app_user (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id        UUID        NOT NULL REFERENCES organization (id),
    email         TEXT        NOT NULL,
    password_hash TEXT        NOT NULL,
    role          TEXT        NOT NULL,
    name          TEXT        NOT NULL,
    avatar_url    TEXT,
    timezone      TEXT        NOT NULL DEFAULT 'America/Argentina/Buenos_Aires',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT app_user_role_check CHECK (role IN ('PLAYER', 'COACH', 'ADMIN')),
    -- Emails are stored lower-cased, so this is a case-insensitive uniqueness guarantee.
    CONSTRAINT app_user_org_email_key UNIQUE (org_id, email)
);

CREATE TRIGGER app_user_set_updated_at
    BEFORE UPDATE ON app_user
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE coach_profile (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id      UUID        NOT NULL REFERENCES organization (id),
    user_id     UUID        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    booking_url TEXT,
    bio         TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT coach_profile_user_key UNIQUE (user_id)
);

CREATE TRIGGER coach_profile_set_updated_at
    BEFORE UPDATE ON coach_profile
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE player_profile (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id           UUID        NOT NULL REFERENCES organization (id),
    user_id          UUID        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    -- Null until a coach takes the player on; the mockup's onboarding is manual.
    coach_id         UUID        REFERENCES coach_profile (id) ON DELETE SET NULL,
    membership_tier  TEXT        NOT NULL DEFAULT 'BASE',
    current_week     INT         NOT NULL DEFAULT 1,
    cycle_started_at DATE,
    program_pdf_key  TEXT,
    next_call_at     TIMESTAMPTZ,
    injury_notes     TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT player_profile_user_key UNIQUE (user_id),
    CONSTRAINT player_profile_tier_check CHECK (membership_tier IN ('BASE', 'PRO')),
    -- The strength cycle in the mockup is four weeks long.
    CONSTRAINT player_profile_week_check CHECK (current_week BETWEEN 1 AND 4)
);

CREATE INDEX player_profile_coach_idx ON player_profile (coach_id);

CREATE TRIGGER player_profile_set_updated_at
    BEFORE UPDATE ON player_profile
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Refresh tokens rotate: every use revokes the presented token and issues a new one.
-- Only the hash is stored, so a database leak does not hand over live sessions.
CREATE TABLE refresh_token (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id         UUID        NOT NULL REFERENCES organization (id),
    user_id        UUID        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    token_hash     TEXT        NOT NULL,
    -- Chains a rotation together so that replaying an old token can revoke the whole family.
    family_id      UUID        NOT NULL,
    issued_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at     TIMESTAMPTZ NOT NULL,
    revoked_at     TIMESTAMPTZ,
    CONSTRAINT refresh_token_hash_key UNIQUE (token_hash)
);

CREATE INDEX refresh_token_user_idx ON refresh_token (user_id);
CREATE INDEX refresh_token_family_idx ON refresh_token (family_id);
