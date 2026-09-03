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

`docker-compose.yml` with Postgres, MinIO (standing in for R2) and Mailpit. Cloudflare Stream has
no emulator, so development runs against a real development account using short test videos.

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
└── player/
    ├── application/                     PlayerHomeService · dto/PlayerHomeDtos
    └── infrastructure/web/              PlayerController
```

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

- `organization` — the business
- `app_user` — email, password_hash, role (`PLAYER` | `COACH` | `ADMIN`), name, avatar, timezone
- `coach_profile` — user_id, booking_url, bio
- `player_profile` — user_id, coach_id, `membership_tier` (`BASE` | `PRO`), current_week,
  cycle_started_at, program_pdf_key, next_call_at, injury_notes
- `refresh_token` — rotating, revocable

### Video and analysis

- `video_asset` — stream_uid, playback_id, `kind` (`PLAYER_UPLOAD` | `COACH_FEEDBACK` |
  `LIBRARY_DRILL` | `PROTOCOL`), `status` (`PENDING_UPLOAD` → `UPLOADING` → `PROCESSING` →
  `READY` | `ERRORED`), duration, thumbnail_url, size_bytes
- `video_submission` — player_id, video_asset_id, `stroke_type`, player_note, recorded_at,
  `status` (`SUBMITTED` | `IN_REVIEW` | `ANALYZED`)
- `analysis` — **nullable submission_id** (the coach sometimes films the player themselves),
  player_id, coach_id, title, summary, feedback_video_asset_id, `status` (`DRAFT` | `PUBLISHED`)
- `analysis_finding` — analysis_id, **timestamp_seconds**, title, detail, sort_order — the
  `0:42` / `4:15` / `9:30` marks in the mockup

### Drills

- `video_library_item` — global per organisation: title, cue, category, video_asset_id
- `drill_assignment` — player_id, library_item_id, **nullable analysis_id**, coach_note,
  sort_order. The join table from the mockup; `analysis_id` closes the loop, letting the app say
  "these drills come from your analysis".

### Strength and conditioning

- `cycle_phase` — global, four rows: week_number, phase_name, goal, sets_reps, rpe_range
- `training_day` — player_id, day_label, title, focus, duration, `kind` (`REHAB` | `STRENGTH`)
- `day_exercise` — training_day_id, library_item_id, sets_reps_wk1..wk4, cue_override, sort_order

### Tracking and communication

- `session_log` — player_id, date, session_type, what_you_did, rpe, pain_score, notes. The player
  creates rows and can then only read them.
- `coach_note` — player_id, body, published_at
- `red_flag` — **nullable player_id**, so a row is either global or player-specific
- `protocol` — title, type (`WARMUP` | `RECOVERY`), pdf_object_key, video_asset_id
- `notification` and `push_token` (user_id, expo_token, platform)

**Booking** is not built. `coach_profile.booking_url` plus `player_profile.next_call_at`, with
Cal.com embedded — the same approach as the mockup.

## The video flow

This has the most moving parts and is where the product is won or lost. **Video bytes never pass
through the backend.**

1. `POST /api/v1/submissions/upload-intent` — the backend requests a Stream *direct creator
   upload* with `requireSignedURLs: true`, `maxDurationSeconds`, `creator: playerId` and
   `meta: { submissionId }`. It creates a `video_asset` in `PENDING_UPLOAD` and a
   `video_submission` in `SUBMITTED`, and returns the one-time `uploadURL`.
2. The mobile app uploads via `tus` straight to Cloudflare, with a progress bar, resuming if the
   connection drops.
3. Cloudflare transcodes, then calls `POST /api/v1/webhooks/cloudflare-stream`. **Verify the HMAC
   signature header and make the update idempotent** — Cloudflare retries. The asset moves to
   `READY` with duration and thumbnail, and the coach gets a push notification.
4. The coach opens the pending queue in Electron, plays the video through a signed URL, and
   downloads the original to edit in their own tool.
5. Back in the app, the coach creates the `analysis`, uploads the edited MP4 through the same
   mechanism with `kind=COACH_FEEDBACK`, marks findings with the "mark here" button, assigns
   drills from the library, and publishes.
6. Publishing sends push and email. The player opens the analysis and sees the video with a list
   of clickable findings.

### Privacy

`requireSignedURLs: true` always — client videos are sensitive data. The backend issues
short-lived playback tokens (around two hours) only after verifying that the requesting user is
entitled to that asset. No `playback_id` is ever served unprotected.

### Quotas

`membership_tier` gates features — the mockup already shows this, where a base-tier client never
sees the S&C tile — and also caps analyses per month. No billing in v1; the tier is set manually.

## Phases

Each phase ends with something usable across all three surfaces, rather than one horizontal layer.

**Phase 0 — Foundations.** Move Gradle into `backend/`, correct the group to
`com.barbonperformance`, pin versions. Spring Boot running against Postgres with Flyway. Docker
Compose. A pnpm workspace with both apps scaffolded. A working `gen:api` pipeline.

**Phase 1 — Auth and profiles.** Registration and login, JWT with rotating refresh tokens, roles
and role guards. Login on both clients. Player home showing their name and current week; coach
client list.

**Phase 2 — The video loop.** *The phase that defines the project — get it right before moving
on.* Stream integration, upload intent, `tus` on both clients, idempotent webhook, signed
playback. Coach analysis queue, finding editor with "mark here", publishing. Player view with
clickable findings that seek the video.

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
