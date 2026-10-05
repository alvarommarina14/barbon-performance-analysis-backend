# Barbon Performance Analysis — Architecture

Barbon Performance runs tennis coaching built on biomechanical video analysis. The product is
already validated as a no-code mockup (Softr + Notion), kept at
`docs/barbon-performance-analysis-mockup-v3.html` in the
`barbon-performance-analysis-frontend` repository, which defines the navigation, the annotated
data model and the real product copy. This document
describes the architecture that replaces it with a purpose-built product.

The core loop is: **the player records themselves on court → uploads the video → the coach
analyses it → the player receives an edited feedback video with findings marked by timestamp.**
Around that loop lives the rest of the coaching platform the mockup already shows: assigned
drills, a 4-week strength cycle, training days, a session log, coach notes, red flags,
warm-up/recovery protocols and call booking.

Two distinct clients: **player on mobile**, **coach on desktop**.

## Decisions

| Decision | Choice | Consequence |
|---|---|---|
| Feedback production | The coach edits externally (Coach's Eye / DaVinci) and uploads the MP4 | The app is a CRM + video pipeline + timestamped findings. No in-app telestration editor, no server-side rendering. |
| v1 scope | The full mockup | Built in six phases, each one shippable. |
| Coach app | Electron | React + Vite, packaged, with local filesystem access for the edited MP4s. |
| Video infrastructure | Cloudflare R2 + Stream | No egress charges, HLS transcoding included, signed URLs for privacy. |
| Coach onboarding | Self-serve from the desktop app, one per org for now, guarded in the application layer | No booking/bio at signup; a player's coach is resolved by the backend. See [ADR 0002](adr/0002-coach-onboarding.md). |

### Note on the feedback decision

Editing outside the app means the coach works with two tools, and every correction means
re-exporting and re-uploading. The data model below keeps `analysis` decoupled from the video
asset and stores findings as rows with a timestamp rather than burned into the MP4. If in-app
annotation is wanted later, it is a new table, not a schema rewrite.

That same decoupling should be exploited in v1: findings are **clickable** — tapping `0:42` seeks
the player to that point. This is what separates the product from sending an MP4 over WhatsApp.

## Stack

### Backend — `backend/`

- **Java (LTS) + Spring Boot + Gradle** — exact versions pinned in Phase 0 against what is
  currently stable, not copied from another project
- Spring Web · Spring Security + JWT (jjwt) · Spring Data JPA · Bean Validation
- **PostgreSQL + Flyway** — versioned migrations from the first table onward
- **springdoc-openapi** — the generated spec is the source of truth for the client contract
- **AWS SDK v2 (S3 client)** pointed at R2 (S3-compatible) for PDFs, images and audio
- A dedicated HTTP client for the Cloudflare Stream API
- **Testcontainers** (real Postgres in integration tests) + **WireMock** (Cloudflare API)
- **MapStruct** for entity↔DTO mapping — with roughly twenty entities, hand-written mappers do
  not scale

### Mobile, player — `frontend/apps/mobile/`

- **React Native + Expo**, TypeScript
- `expo-router` for file-based routing · `expo-camera` / `expo-image-picker` to record or pick
  from the camera roll
- `expo-video` to play Stream HLS
- **TanStack Query** for fetching and cache · `expo-secure-store` for tokens
- **`tus-js-client`** for resumable uploads — non-negotiable: uploads happen courtside over
  mobile data
- `expo-notifications` for push

### Desktop, coach — `frontend/apps/coach/`

- **Electron + React + Vite** (`electron-vite` toolchain), TypeScript
- Native file dialog to pick the edited MP4, and the same `tus` client to upload it
- A player with a frame-accurate scrubber and a **"mark here"** button that captures the current
  timestamp and opens the finding form. This detail decides whether the coach uses the app at all.

### Shared — `frontend/packages/`

- `api-client/` — TypeScript types generated from the backend OpenAPI spec (`openapi-typescript`)
  plus a fetch wrapper handling token refresh. A `pnpm gen:api` script regenerates them: when the
  backend changes a DTO, both clients break at compile time.
- `core/` — enums, timestamp formatting, validation, and the **design tokens** extracted from the
  mockup (black `#0a0a0a`, lime `#c8ff00`, Bebas Neue + DM Sans). The mockup is the design system.

### Local infrastructure — `infra/`

`docker-compose.yml` with Postgres, MinIO (standing in for R2), Mailpit, and `tusd`. Cloudflare
Stream has no emulator; development uses the **local video provider** instead
(`analysis.video.provider=local`) — `tusd` + MinIO in place of Stream. Production uses
Cloudflare. See [ADR 0004](adr/0004-local-video-provider.md) and the README.

## Repository layout

Two repositories, cloned side by side. Every document has exactly one home: this one and the
ADRs live with the backend, the mockup lives with the clients that are built from it.

```
barbon-performance-analysis-backend/
├── src/              # Spring Boot, Flyway migrations
├── infra/            # docker-compose, seed scripts
└── docs/             # this document, and the ADRs

barbon-performance-analysis-frontend/
├── apps/mobile/
├── apps/coach/
├── packages/{api-client,core}/
└── docs/             # the mockup, as living reference
```

## Backend structure

Ports and adapters (hexagonal). Each **module** is a slice of the product, and inside it the
three layers carry their canonical names: `domain/` and `application/` are the inside of the
hexagon, `infrastructure/` is everything outside it, split by the kind of adapter.

```
com.barbonperformance.analysis
├── shared/
│   ├── domain/                          NotFoundException, ConflictException
│   └── infrastructure/
│       ├── web/                         ApiExceptionHandler, ProblemDetails
│       └── config/                      OpenApiConfig
├── organization/
│   ├── domain/                          Organization · Organizations (port)
│   └── infrastructure/persistence/      OrganizationJpaRepository, JpaOrganizations
├── user/
│   ├── domain/                          AppUser, PlayerProfile, CoachProfile, UserRole,
│   │                                    MembershipTier · AppUsers, PlayerProfiles,
│   │                                    CoachProfiles (ports)
│   └── infrastructure/persistence/      *JpaRepository + Jpa* adapters
├── auth/
│   ├── SecurityProperties.java          module configuration, outside every layer
│   ├── domain/                          RefreshToken, AuthenticatedUser,
│   │                                    InvalidRefreshTokenException
│   │                                    RefreshTokens, AccessTokens (ports)
│   ├── application/                     AuthService, RefreshTokenService
│   │   └── dto/                         AuthDtos
│   └── infrastructure/
│       ├── web/                         AuthController, AuthExceptionHandler
│       ├── persistence/                 RefreshTokenJpaRepository, JpaRefreshTokens
│       └── security/                    JwtAccessTokens, JwtAuthenticationFilter,
│                                        SecurityConfig
├── coach/
│   ├── application/                     CoachService · dto/CoachDtos
│   └── infrastructure/web/              CoachController
├── player/
│   ├── application/                     PlayerHomeService · dto/PlayerHomeDtos
│   └── infrastructure/web/              PlayerController
├── video/
│   ├── VideoProperties.java             module configuration, outside every layer
│   ├── domain/                          VideoAsset, VideoSubmission, StreamEvent, the enums ·
│   │                                    VideoAssets, VideoSubmissions, StreamGateway,
│   │                                    StreamWebhooks (ports)
│   ├── application/                     VideoUploadService, PlaybackService,
│   │   └── dto/                         StreamWebhookService · VideoDtos
│   └── infrastructure/
│       ├── web/                         VideoController, StreamWebhookController, exception advice
│       ├── persistence/                 *JpaRepository + Jpa* adapters
│       └── gateway/                     CloudflareStreamGateway, CloudflareStreamWebhooks
└── analysis/
    ├── domain/                          Analysis (aggregate, owns AnalysisFinding), AnalysisStatus ·
    │                                    Analyses (port) · module exceptions
    ├── application/                     AnalysisService (coach commands), AnalysisReadService,
    │   └── dto/                         AnalysisPlaybackService · AnalysisDtos
    └── infrastructure/
        ├── web/                         AnalysisController, AnalysisExceptionHandler
        └── persistence/                 AnalysisJpaRepository, JpaAnalyses
```

`analysis` reuses `video/domain`'s ports (`StreamGateway`, `VideoAssets`, `VideoSubmissions`) and
`VideoProperties` directly from its application layer — an `analysis → video` module edge, with no
edge back, so `ArchitectureTest`'s no-cycles rule holds. The feedback video is a plain
`video_asset` of `kind = COACH_FEEDBACK`; `analysis` owns its playback entitlement rather than
teaching `video` about analyses (see [ADR 0003](adr/0003-playback-entitlement-without-a-module-cycle.md)).

Every module from Phase 2 onward repeats the skeleton, adding `infrastructure/gateway/` for the
driven adapters that talk to a third party — `video/infrastructure/gateway/CloudflareStreamAdapter`,
`R2ObjectStorage`. The port they implement lives in `video/domain/`, which is what lets the
analysis use cases run against WireMock, or against a hand-written fake, with no HTTP client in
the way.

### The rules

- **`domain/` depends on nothing around it.** It holds the entities and the **ports**: interfaces
  named for what the module needs (`AppUsers`, `AccessTokens`, later `StreamGateway`), owned by
  the side that needs them rather than by the technology behind them. That ownership, not the
  folder names, is what makes this hexagonal.
- **`application/` orchestrates ports** and owns `@Transactional`. It never names a class under
  `infrastructure/`. Its DTOs live in `application/dto/` because they are the contract of the use
  case; putting them in the web adapter would invert the dependency.
- **`infrastructure/web/` is the driving adapter**: HTTP in, DTO out, and it calls use cases.
- **`infrastructure/persistence/`, `/security/` and later `/gateway/` are driven adapters.** They
  implement a port and are called through it, so they never reference `application/`. They are
  package-private: nothing outside the package can even name them.
- **Spring Data exists only inside `infrastructure/persistence/`.** A `JpaRepository` is an
  implementation detail; everything else sees the port.

### Entities keep their JPA annotations

A deliberate trade-off. The persistence-free version — plain domain objects, separate `@Entity`
classes, MapStruct in both directions — roughly doubles the classes per module and makes every
`join fetch` a remapping exercise. What buys the decoupling here is the **port**, not the absence
of `@Entity`, because the parts of Phase 2 that need to be swapped and faked are Cloudflare and
R2, not Postgres.

Two other leaks are accepted on the same grounds: `PasswordEncoder` and `BadCredentialsException`
from Spring Security appear in `application/`. They are stable library abstractions, and wrapping
them would be ceremony rather than insulation.

`ArchitectureTest` (ArchUnit) enforces all of the above, including that the module slices stay
free of cycles, and fails the build. The rules are written against the `infrastructure/` boundary
rather than against a list of adapter package names, so an adapter kind that does not exist yet —
`gateway/` — is governed from the day it is created.

## Data model

A direct translation of the annotated "Notion data map" in the mockup to Postgres. Every table
carries `org_id` from day one — there is only one organisation today, but adding it later is a
painful migration.

### Identity

- `organization` — the coaching business. One today; it is expected to hold several coaches over
  time, and every table is `org_id`-scoped for that.
- `app_user` — email, password_hash, role (`PLAYER` | `COACH` | `ADMIN`), name, avatar, timezone
- `coach_profile` — user_id, booking_url, bio
- `player_profile` — user_id, coach_id, `membership_tier` (`BASE` | `PRO`), plan_started_at,
  program_pdf_key, next_call_at, injury_notes
- `refresh_token` — rotating, revocable

The coach self-registers from the desktop app (`POST /api/v1/auth/register-coach`). It is open
but rejects a second coach in the org — a Phase-1 bootstrap guard in the application layer that
an invite flow replaces later. A player's coach is resolved by the backend at registration (the
org's coach, or NULL until one exists); the client never sends a coach id. See
[ADR 0002](adr/0002-coach-onboarding.md).

### Video and analysis

- `video_asset` — stream_uid, playback_id, `kind` (`PLAYER_UPLOAD` | `COACH_FEEDBACK` |
  `LIBRARY_DRILL` | `PROTOCOL` | `RECORDING_TIPS`), `status` (`PENDING_UPLOAD` → `UPLOADING` →
  `PROCESSING` → `READY` | `ERRORED`), duration, thumbnail_url, size_bytes
- `video_submission` — player_id, video_asset_id, player_note, recorded_at, `status`
  (`SUBMITTED` | `IN_REVIEW` | `ANALYZED`). No stroke on the row — analysis always covers every
  stroke in the clip.
- `analysis` — **nullable submission_id** (the coach sometimes films the player themselves),
  player_id, coach_id, title, summary, feedback_video_asset_id, `status` (`DRAFT` | `PUBLISHED`)
- `analysis_finding` — analysis_id, **timestamp_seconds**, title, detail, sort_order — the
  `0:42` / `4:15` / `9:30` marks in the mockup

### Drills

- `video_library_item` — global per organisation (shared across every coach's clients): title,
  cue, `category` (`FOREHAND` | `BACKHAND` | `SERVE` | `MOVEMENT` | `FITNESS`), video_asset_id
  (`kind=LIBRARY_DRILL`, uploaded through the same pipeline as any other video)
- `drill_assignment` — player_id, library_item_id, **nullable analysis_id**, coach_note,
  sort_order. The join table from the mockup; `analysis_id` closes the loop, letting the app say
  "these drills come from your analysis". Every write path today sets it; the column stays
  nullable for a future drill assigned outside of any analysis.

The coach manages the library and assigns drills from `apps/coach`:

- `GET /api/v1/drills/library` — the organisation's whole library.
- `POST /api/v1/drills/library/upload-intent` then `POST /api/v1/drills/library` once the asset is
  `READY` (retryable `409 video-not-ready` while it isn't) — same upload-before-create shape as
  submissions ([ADR 0005](adr/0005-upload-before-submission.md)).
- `PUT /api/v1/drills/library/{itemId}` / `DELETE /api/v1/drills/library/{itemId}` (rejected with
  `409` while the drill is still assigned somewhere).
- `GET /api/v1/drills/library/{itemId}/playback` — org-wide visibility, no per-player entitlement:
  the library is shared, not owned by whoever assigned it.
- `GET /api/v1/drills/assignments?category=&analysisId=&playerProfileId=` — a player's own
  drills, one analysis' drills, or (coach only) one client's drills.
- `PUT /api/v1/drills/analyses/{analysisId}/assignments` — replaces the whole drill list on one
  analysis; the request array's order becomes `sort_order`.

### Strength and conditioning

- `cycle_phase` — **per-player**, not shared: `org_id`, `player_id` (nullable), `template_name`
  (nullable), `week_number`, `phase_name`, `goal`, `sets_reps`, `rpe_range`. Exactly one of
  `player_id`/`template_name` is set per row — a row belongs either to a player's own cycle or to
  a reusable, named org-level template, never both. A template is nothing more than four
  `cycle_phase` rows with no player, grouped and addressed by `(org_id, template_name)` instead of
  a surrogate id; applying one to a player is a one-time copy into that player's own rows, not a
  live link — editing either side afterward never affects the other.
- `training_day` — org_id, player_id, day_label, title, focus, duration_minutes, `kind`
  (`REHAB` | `STRENGTH`), sort_order. day_label is not typed by the coach: it is always
  "Day {sort_order + 1}", so adding, removing or reordering days renumbers them (V17 renumbered
  the days that already had free-text labels).
- `day_exercise` — org_id, training_day_id, library_item_id (reuses `video_library_item` from
  Drills, category `FITNESS`; no second video pipeline), sets_reps_wk1..wk4, cue_override,
  sort_order

The `strength` module owns all three. The coach manages templates, a client's cycle and a client's
training days from `apps/coach`:

- `GET /api/v1/strength/cycle-phases?playerProfileId=` — a player's own four weeks, or (coach
  only) one client's.
- `PUT /api/v1/strength/players/{playerProfileId}/cycle-phases` — coach only; replaces that
  player's four weeks directly; the request must contain exactly weeks 1-4, once each (`409`
  otherwise); upserts by `(player_id, week_number)`.
- `POST /api/v1/strength/players/{playerProfileId}/cycle-phases/apply-template` — coach only, body
  `{ templateName }`; copies the named template's weeks into that player's own rows (`404` if the
  name doesn't exist in the coach's org).
- `GET /api/v1/strength/cycle-phase-templates` — coach only; every template in the org, weeks
  nested inline.
- `PUT /api/v1/strength/cycle-phase-templates/{name}` — coach only; creates the template if the
  name is new or replaces it if it already exists (upsert by name); the request must contain
  exactly weeks 1-4, once each (`409` otherwise). There is no separate create/rename step — a
  template is always written whole.
- `DELETE /api/v1/strength/cycle-phase-templates/{name}` — coach only; `404` if the name doesn't
  exist. Deleting a template never touches any player's already-copied rows.
- `GET /api/v1/strength/training-days?playerProfileId=` — a player's own days, or (coach only) one
  client's days.
- `PUT /api/v1/strength/players/{playerProfileId}/training-days` — sets the whole day list for
  that player; the request array's order becomes `sort_order`. An item carrying an `id` updates that
  day in place, one without is new, and days left out are deleted. Updating in place matters: a
  delete-and-recreate would cascade away the day's exercises and the player's logs.
- `GET /api/v1/strength/training-days/{trainingDayId}/exercises` — a day's exercise list.
- `PUT /api/v1/strength/training-days/{trainingDayId}/exercises` — sets the whole exercise
  list on one day, with the same `id` rules as the day list; each `libraryItemId` is resolved against the same-org `video_library_item`
  table, without forcing category `FITNESS` server-side (that filter lives in the coach's picker
  UI only, same convention as Drills).
- The exercise video plays through the existing `GET /api/v1/drills/library/{itemId}/playback` —
  no new playback endpoint.

**The twelve-week plan.** A plan runs twelve weeks: the four-week cycle three times over.
`player_profile.plan_started_at` is the only stored fact; the plan week (1-12) is derived from it
and today's date in the player's timezone (`user.domain.PlanCalendar`), and the cycle week (1-4)
from the plan week. There is no stored "current week" to fall out of date. `PUT
/api/v1/strength/players/{playerProfileId}/plan { startDate }` (coach only; null clears it) and `GET
/api/v1/strength/plan?playerProfileId=` return `{ startDate, planWeek, planWeeks, cycleWeek,
unlockedWeeks }`; `planWeek` is null before the start and after week 12, and `unlockedWeeks` is how
many weeks the player has reached (0-12). The cycle week picks out which of the player's own
`cycle_phase` rows is "now"; `GET /api/v1/players/me/home` returns it as `currentPhase` (nullable —
null outside the plan or until that week's row is loaded). Shown to every membership tier today; BASE/PRO gating on this card is deliberately not
built yet.

**Program PDF.** `player_profile.program_pdf_key` (existing since Phase 1, unused until now) is a
plain object behind a presigned PUT, at a deterministic key per player
(`content/{orgId}/players/{playerId}/program.pdf`), fixed content-type `application/pdf`. `POST
/api/v1/players/{playerProfileId}/program-pdf/upload-intent` + `POST .../confirm` (coach only);
`GET /api/v1/players/me/home` returns the current signed download link as `programPdfUrl`.

**RPE guide.** One PDF per organisation, undocumented in the original mockup but built as a
sibling of Recording tips minus the video half: `rpe_guide` (org_id unique, pdf_object_key,
pdf_content_type, updated_by_user_id, updated_at). `GET /api/v1/content/rpe-guide` — `{ pdfUrl?,
updatedAt? }`, null when nothing has been uploaded yet. `POST
/api/v1/content/rpe-guide/pdf-upload-intent` + `POST .../pdf` (confirm) — coach only, reusing the
same `content.domain.ObjectStorage` port and the generic upload-intent/confirm DTOs Recording tips
already defined.

### Tracking and communication

- `session_log` — player_id, date, session_type, what_you_did, rpe, pain_score, notes. The player
  creates rows and can then only read them.
- `exercise_log` — player_id, day_exercise_id, plan_week, unit (`LB` | `KG`), logged_at; unique per
  player, exercise and week, with its sets in `exercise_log_set` (set_number, reps, weight; a null
  weight is bodyweight). `PUT /api/v1/strength/day-exercises/{id}/logs/{planWeek} { unit, sets }`
  is player only, for their own exercises and only weeks they have reached (`409` otherwise); no
  sets deletes the log (`204`). `GET /api/v1/strength/training-days/{id}/logs?planWeek=` serves the
  player and their coach.
- `weekly_note` — player_id, plan_week (1-12, unique per player), body. Built in place of the
  planned `coach_note`: the coach's message is tied to a week of the plan. `GET
  /api/v1/strength/weekly-notes?playerProfileId=` returns a player only the weeks they have
  reached, so notes written ahead never leak; the coach sees all twelve. `PUT
  /api/v1/strength/players/{playerProfileId}/weekly-notes/{planWeek} { body }` writes one week (coach
  only); an empty body deletes it. Each note carries `weekStartsOn`, derived from the plan start.
- `red_flag` — **nullable player_id**: a row without one is an entry in the org's red flag
  library, a row with one belongs to that player; title, body, sort_order. The library works like
  cycle phase templates: the coach picks entries for each client and the pick is a one-time copy
  into the player's rows, which the coach can then edit or extend with ones specific to that
  player's condition. A player only ever sees their own rows, never the library. (V14's comment
  still describes the first design, where library rows applied to every client.) `GET
  /api/v1/strength/red-flags?playerProfileId=` returns the player's list; coach-only `GET`/`PUT
  /api/v1/strength/red-flag-library` and `PUT /api/v1/strength/players/{playerProfileId}/red-flags`
  replace each list whole — safe here because nothing references a red flag.
- `daily_routine` — one row per org: warmup_video_asset_id, recovery_video_asset_id (both
  `video_asset` kind `PROTOCOL`) and one PDF with its file details. Built in place of the planned
  multi-row `protocol` table, since the product has exactly these three pieces. `GET
  /api/v1/content/daily-routine`, `GET .../playback?part=WARMUP|RECOVERY` (its own entitlement,
  like recording tips), and coach-only `POST .../video-upload-intent { part, sizeBytes, fileName }`,
  `POST .../video { part, videoAssetId }`, `POST .../pdf-upload-intent`, `POST .../pdf`. Every PDF
  confirm (daily routine, recording tips, RPE guide, program PDF) rejects an object key other than
  the one its upload intent handed out, with `409`.
- `notification` and `push_token` (user_id, expo_token, platform)

**Booking** is a link, the way a recruiter sends one: the coach pastes the URL of their booking
page (a Google Calendar appointment schedule, Calendly…) and the player's app opens it. `GET` and
`PUT /api/v1/coach/booking-link { bookingUrl }` (coach only; https only, blank clears it) write
`coach_profile.booking_url`, which `GET /api/v1/players/me/home` already returns in `coach`. The app
does not read the coach's calendar, so it cannot know when the next call is: syncing with Google
Calendar, a one-call-every-14-days rule and a "Your next call" date (`player_profile.next_call_at`,
still unwritten) are deferred until that rule is defined.

### Recording tips

The instructional "how to record" video and PDF, one per organisation, both coach-uploadable so
they can be replaced without a deploy:

- `recording_tips` — org_id (unique), video_asset_id (`kind=RECORDING_TIPS`, same video pipeline
  as a drill), pdf_object_key, pdf_content_type, updated_by_user_id, updated_at
- `GET /api/v1/content/recording-tips` — `{ videoAssetId?, videoStatus?, pdfUrl?, updatedAt? }`,
  nulls when nothing has been uploaded yet rather than a 404. The video plays through the
  ordinary `GET /api/v1/videos/{videoAssetId}/playback`; the PDF's `pdfUrl` is already a
  short-lived signed download link.
- `POST /api/v1/content/recording-tips/video-upload-intent` + `POST .../video` (confirm), and
  `POST .../pdf-upload-intent` + `POST .../pdf` (confirm) — coach only. The PDF is a plain object
  (MinIO locally, Cloudflare R2 in production) behind a presigned PUT, at a deterministic key per
  org (`content/{orgId}/recording-tips/guide.pdf`) so a re-upload just overwrites it — see the
  `content` module's `ObjectStorage` port, the first user of the `R2ObjectStorage`-shaped gateway
  this doc anticipated for PDFs, images and audio.

## The video flow

This has the most moving parts and is where the product is won or lost. **Video bytes never pass
through the backend.**

1. `POST /api/v1/submissions/upload-intent` (with the file's `sizeBytes`) — the backend creates a
   **resumable (tus) direct creator upload**: `POST /accounts/{id}/stream` with
   `Tus-Resumable: 1.0.0`, `Upload-Length`, `Upload-Creator: playerId` and an `Upload-Metadata`
   carrying `requiresignedurls`, `maxDurationSeconds` and `videoAssetId`. Cloudflare returns the
   one-time URL in the `Location` header and the id in `stream-media-id`. The backend creates
   **only a `video_asset`** (in `PENDING_UPLOAD`, stamped with the player) and returns that URL as
   `uploadUrl`. No `video_submission` yet — see [ADR 0005](adr/0005-upload-before-submission.md).
2. The mobile app uploads via `tus` straight to that URL (no credential — the URL is the
   authentication), with a progress bar, resuming if the connection drops.
3. Cloudflare transcodes, then calls `POST /api/v1/webhooks/cloudflare-stream`. **Verify the HMAC
   signature header and make the update idempotent** — Cloudflare retries. The asset moves to
   `READY` with duration and thumbnail.
4. `POST /api/v1/submissions` (`{ videoAssetId, playerNote?, recordedAt? }`) — the
   client sends this once its upload has succeeded. The backend checks the asset is the player's
   and `READY` (retryable `409 video-not-ready` while the webhook is still in flight) and creates
   the `video_submission` in `SUBMITTED`. It now shows in `GET /api/v1/submissions/queue`. An
   asset that never gets a submission is swept after `analysis.video.orphaned-upload-grace`.
6. The coach opens the pending queue in Electron, plays the video through a signed URL, and gets
   the original MP4 from `GET /api/v1/videos/{assetId}/download-url` (Cloudflare renders the
   download lazily; the response says whether the URL is usable yet) to edit in their own tool.
7. Back in the app, the coach creates the `analysis` (`POST /api/v1/analyses`, which moves the
   submission to `IN_REVIEW`), uploads the edited MP4 through the same tus direct-upload + webhook
   mechanism (`POST /api/v1/analyses/{id}/feedback-upload-intent` with `sizeBytes`,
   `kind=COACH_FEEDBACK`, `Upload-Metadata` `analysisId`), replaces the whole finding
   list with `PUT /api/v1/analyses/{id}/findings` (each `{ timestampSeconds, title, detail }`),
   and publishes (`POST /api/v1/analyses/{id}/publish`, which requires the feedback video to be
   `READY` and moves the submission to `ANALYZED`). The coach can also assign drills to the
   analysis at this point (`PUT /api/v1/drills/analyses/{id}/assignments`) — see Drills, above.
8. The player polls `GET /api/v1/analyses`, opens one, and plays the feedback video through
   `GET /api/v1/analyses/{id}/playback` with a list of clickable findings. **Phase 2 sends no
   push or email** — that is Phase 6; until then both sides poll.

### The local provider

`analysis.video.provider` picks the adapter behind `StreamGateway` / `StreamWebhooks`:
`cloudflare` (production, unchanged) or `local`. In `local` mode the steps above are
identical from the client's point of view, but:

- the tus creation `POST` goes to **`tusd`**, which stores the finished upload in the **MinIO**
  bucket. `tusd` runs `-behind-proxy` so the upload URL it advertises is the one the phone can
  reach (`analysis.video.local.public-base-url`).
- step 3 is `tusd`'s `post-finish` hook to `POST /api/v1/webhooks/tusd`, authenticated by a
  shared secret in the query string. There is no transcode, so **duration and thumbnail are
  null**.
- playback and download (steps 4, 6) are **pre-signed MinIO GET URLs** for the stored MP4 —
  no HLS, no separate token.

Details and trade-offs: [ADR 0004](adr/0004-local-video-provider.md).

### Privacy

`requireSignedURLs: true` always — client videos are sensitive data. The backend issues
short-lived playback tokens (around two hours) only after verifying that the requesting user is
entitled to that asset. No `playback_id` is ever served unprotected.

Entitlement is owned by the module that owns the asset's context: `video`'s
`/api/v1/videos/{id}/playback` serves the player upload (player owns it, or the coach owns the
player), and `analysis`'s `/api/v1/analyses/{id}/playback` serves the feedback video (player if
the analysis is published and theirs, coach if it is theirs). Neither module reaches into the
other — see [ADR 0003](adr/0003-playback-entitlement-without-a-module-cycle.md).

### Quotas

`membership_tier` gates features — the mockup already shows this, where a base-tier client never
sees the S&C tile. No billing in v1; the tier is set manually. An analyses-per-month cap is
mentioned as a future lever but no number is specified, so **Phase 2 implements no cap**.

## Phases

Each phase ends with something usable across all three surfaces, rather than one horizontal layer.

**Phase 0 — Foundations.** Move Gradle into `backend/`, correct the group to
`com.barbonperformance`, pin versions. Spring Boot running against Postgres with Flyway. Docker
Compose. A pnpm workspace with both apps scaffolded. A working `gen:api` pipeline.

**Phase 1 — Auth and profiles.** Registration and login, JWT with rotating refresh tokens, roles
and role guards. Player registration on mobile; coach self-registration on the desktop app, one
per org (a bootstrap guard the later invite flow removes). A player is assigned the org's coach
at registration. Login on both clients. Player home showing their name and current week; coach
client list.

**Phase 2 — The video loop.** *The phase that defines the project — get it right before moving
on.* Stream integration, upload intent, `tus` on both clients, idempotent webhook, signed
playback, original-video download. The `analysis` module: coach review queue, draft analysis,
`kind=COACH_FEEDBACK` upload, bulk finding editor with "mark here", publish. Player view with
clickable findings that seek the video. No push/email (Phase 6); no analyses cap.

**Phase 3 — Drills.** Video library, assignment from an analysis, category filters, per-drill
coach notes.

**Phase 4 — Strength and conditioning.** The 4-week cycle with the current week highlighted,
training days, exercises with per-week sets and reps, program PDF served from R2.

**Phase 5 — The rest of the mockup.** Session log (form plus read-only history), coach notes, red
flags, warm-up and recovery protocols, booking embed.

**Phase 6 — Notifications and polish.** Expo push, transactional email, notification centre, empty
states, network error handling, and Electron packaging and signing.

## Verification

### Per phase

- Backend: integration tests with Testcontainers against real Postgres, with the Cloudflare API
  mocked through WireMock. Webhook tests cover an invalid signature and a duplicate delivery.
- Contract: `pnpm gen:api` regenerates the types. If a client stops compiling, the contract changed.

### End-to-end video flow

This run must pass in full before Phase 2 is considered done.

1. `docker compose up`, plus the backend and both clients in dev mode.
2. From the mobile app on a real device — a simulator has no camera and no mobile network — record
   30 seconds, upload, and **kill the network mid-upload** to confirm `tus` resumes.
3. Confirm in the Cloudflare dashboard that the video processed, and in Postgres that the webhook
   left the asset `READY` with duration and thumbnail.
4. Replay the same webhook by hand with curl and confirm nothing is duplicated.
5. In Electron: open the queue, play the video, download the original, upload a feedback MP4, mark
   three findings, assign drills, publish.
6. On mobile: receive the push, open the analysis, tap `0:42`, and confirm the player seeks there.
7. Log in as a **different** player and request the first player's `playback_id` through the API.
   It must return 403.

### Manual

Open `docs/barbon-performance-analysis-mockup-v3.html` from the
`barbon-performance-analysis-frontend` repository alongside the app and compare screen by
screen. The mockup is the specification for both UX and copy.
