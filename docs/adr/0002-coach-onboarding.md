# ADR 0002 — Coach onboarding and coach assignment

Status: accepted (Phase 1)

## Context

Phase 1 shipped player self-registration (`POST /api/v1/auth/register`), but nothing creates a
coach: `AppUser` rows with role `COACH` and their `coach_profile` existed only in test fixtures
or a manual SQL insert. A freshly registered player therefore had `player_profile.coach_id`
NULL, and `GET /api/v1/coach/clients` was always empty.

Two questions had to be answered before building further onboarding: what `organization` means,
and who decides a player's coach.

`organization` is the coaching **business** — Barbon Performance — and is expected to hold
**several coaches** over time (an assistant coach, a specialist). Players belong to the
organisation and are assigned to a coach within it. `org_id` scoping stays on every table. There
is one organisation and one coach today; that is the current state, not a permanent shape.

## Decision

### The coach creates their own account from the desktop app

`POST /api/v1/auth/register-coach` — open (no authentication, like player registration), returns
a session. The request body mirrors `RegisterRequest` (email, password, name, optional
timezone). `bio` and `booking_url` stay NULL; editing the coach profile is a later change.
Forgot-password is out of scope.

### One coach per organisation — a Phase-1 bootstrap guard, not an invariant

`AuthService.registerCoach` rejects a second coach with `409`. This is enforced in the
application layer, not by a schema constraint:

- `AuthService` is the only writer of `coach_profile` in production.
- A `UNIQUE (org_id)` on `coach_profile` would block the test fixtures that build multi-coach
  organisations to exercise client-list scoping (`RoleGuardIT.coachSeesOnlyTheirOwnClients`),
  and would have to be dropped the moment a second coach is allowed.
- It saves a migration.

The guard is the seam that gets **removed** when a coach can invite other coaches — it is not a
rule the rest of the design should lean on.

### A player's coach is resolved by the backend at registration

`AuthService.register` looks up the organisation's coach (`CoachProfiles.findByOrganizationId`,
which returns the *first* coach — a deliberate placeholder while there is only one) and assigns
it to the new `player_profile`. The client never sends a coach id: coach assignment is an
authorisation-relevant relationship and cannot be a client's choice.

If the organisation has no coach yet, `coach_id` stays NULL and registration still succeeds —
`hasCoach = false` is a valid state the player home already renders. This supersedes the
`V2__identity.sql` comment "Null until a coach takes the player on; the mockup's onboarding is
manual": onboarding is no longer manual, but the column is still nullable for the
no-coach-yet window.

## Consequences

- Two coach signups racing each other could both pass the guard (no database backstop). Accepted:
  the organisation onboards one coach, once.
- Players registered while the organisation had no coach are **not** back-filled when a coach
  later signs up. Reassignment is manual until there is a coach-facing "unassigned players"
  surface.
- When multi-coach lands it brings: dropping the `registerCoach` guard, a coach-invite flow, and
  a real player→coach assignment decision (a claim queue, or assignment by the inviting coach).
  The schema — `coach_profile`, `player_profile.coach_id` — does not change.
- `RoleGuardIT` keeps two coaches in one organisation on purpose; that is the direction the
  product is heading, and the fixtures write through the ports below the guard.
