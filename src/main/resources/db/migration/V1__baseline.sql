-- Court Proof baseline schema.
--
-- Every business table carries org_id from the first migration: there is a single
-- organisation today, but retrofitting tenancy onto a populated database is painful.

CREATE TABLE organization (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT        NOT NULL,
    slug        TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT organization_slug_key UNIQUE (slug)
);

-- Shared trigger: keeps updated_at honest without every service remembering to set it.
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER organization_set_updated_at
    BEFORE UPDATE ON organization
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

INSERT INTO organization (name, slug)
VALUES ('Barbon Performance', 'barbon-performance');
