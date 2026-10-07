# ADR 0007 — Generating the program PDFs from the app's data

Status: accepted (2026-10-07) for the Training Program; the Daily Routine PDF is a later step.
Proposed during the client feedback round (item 7); what was decided and built is under
"Decision" at the end.

## Context

Today the program PDF is a file the coach makes elsewhere and uploads per player
(`player_profile.program_pdf_key`, Phase 4). The client's sample PDFs show two documents per
client: a **Training Program** (about the program, the athlete's level, each training day split
into sections, exercises with sets, reps and notes, cardio days) and a **Daily Routine**
(warm-up and recovery). Most of that content now also lives in the app as data. The question is
whether the app can produce those PDFs itself, and what is missing.

## Options

**A. Keep uploading hand-made PDFs.** Zero work, but the PDF and the app drift apart every time
the coach edits the program in one place and not the other.

**B. Generate on the server from HTML templates.** *(recommended)*

- Thymeleaf renders an HTML template with the program's data; OpenHTMLtoPDF turns it into a PDF.
  Both are plain Java libraries, so the backend needs no headless browser and no external
  service. Print CSS (page size, breaks, running header) is enough for the sample layout.
- `GET /api/v1/players/{id}/program.pdf` and `.../daily-routine.pdf` render on demand (they are
  small), or on save into object storage if rendering ever gets slow.
- The coach app gets a "Preview PDF" button; the uploaded PDF stays as a fallback until the
  generated one covers everything the coach needs.

**C. Generate on the client** (a React-PDF document in the coach app). It avoids a backend
dependency, but the player's app could not get a fresh copy without the coach, and two apps
would need the same layout code.

## What the data model is missing

Compared with the sample PDFs:

- **Sections inside a day** — "Injury prevention" versus "Main work". Today a day's exercises are
  one flat list. This needs a `section` on `day_exercise`, or a `day_section` table.
- **Per-exercise notes separate from the cue** — the samples have a short cue and a longer note.
  Today there is only `cue_override`.
- **Cardio days with a protocol text** — a new `TrainingDayKind.CARDIO` with a free-text protocol
  instead of an exercise list.
- **"About your program" text and the athlete's level** — per-player fields that exist nowhere
  today.

Already in the app after this round: red flags, the Daily Routine's videos and PDF, and the
twelve-week plan and weekly notes that date the program.

## Original proposal

Option B, planned as its own piece of work once the four gaps above are agreed with the client:
the schema changes touch the coach's program editor, so they deserve their own plan rather than
riding on this ADR.

## Decision

Option B, for the Training Program only.

- **The generated PDF replaces the upload.** No fallback: V19 drops `program_pdf_key` and the
  upload endpoints are gone. Keeping both would bring back the drift option A suffers from.
- **Rendered on every open, never stored.** The player gets a link signed for two hours
  (`GET /api/v1/strength/program-pdf-link`, or `programPdfUrl` on the home); the document always
  shows the program as it is now.
- **Video links that outlive the download.** A signed URL expires in two hours but a PDF is kept
  for weeks, so each exercise links to `/api/v1/public/exercise-videos/{token}`: a token signed
  per library item without an expiry, which redirects to a fresh playback URL. The cost is that
  rotating the signing secret breaks every PDF already handed out.
- **The four gaps,** as `day_section` (free-form blocks with a title and an optional note),
  `day_exercise.notes`, `TrainingDayKind.CARDIO` with `training_day.protocol`, and a per-player
  `training_program` (athlete level, about, schedule).
- **Templates carry a whole program** — weeks, days, blocks, exercises, about and schedule —
  instead of only the four cycle weeks. Applying one is a one-time copy. The coach creates them
  from a client ("Save as template") or empty from Setup.

The data model and endpoints are in `docs/ARCHITECTURE.md`, under "Strength and conditioning".

## Consequences and effort

- Model additions and coach editor changes: about 3-4 days.
- Templates, rendering endpoint and preview: about 3 days, most of it layout work against the
  sample PDFs.
- New dependencies: `thymeleaf` and `openhtmltopdf` (plus a bundled font for the display
  typeface), declared in `build.gradle` with pinned versions, per ADR 0001.
