# ADR 0006 — Quick corrections as a lighter kind of submission

Status: proposed (POC, client feedback round, item 6). Nothing is built yet.

## Context

Today every player video goes through the full loop: a `video_submission`, a coach `analysis`
with a feedback video, timestamped `analysis_finding`s, assigned drills, then publish. That loop
is the product's core and it is heavy on purpose — a full breakdown is a big piece of the coach's
time.

The client asked for a smaller interaction: the player sends a short clip about one specific
thing ("is my toss better now?") and the coach answers quickly, without building a whole
breakdown. The questions are whether the current model can carry it, what changes, and roughly
how much work it is.

## Options

**A. Reuse the full analysis as it is.** The player submits normally with a note, and the coach
answers with an analysis that simply has no findings and no drills.

- No schema change and almost no code.
- But the coach still walks the whole editor (review, feedback-video step, findings step, drills
  step, publish), the queue can't tell a quick question from a full breakdown, and nothing limits
  how many a player sends. It changes nothing about the coach's cost, which is the point of the
  request.

**B. A quick submission is a submission of another kind, answered by a light analysis.**
*(recommended)*

- `video_submission.kind` — `FULL` | `QUICK`, default `FULL`. Optionally
  `parent_analysis_id`, so a quick question can hang off the breakdown it follows up on.
- The upload flow does not change (ADR 0005): upload intent, upload, `POST /api/v1/submissions`
  with `kind: QUICK`. The clip is capped shorter (`analysis.video.quick-max-duration-seconds`).
- The answer is still an `analysis` row, so playback, the player's history and entitlements keep
  working unchanged. A quick analysis carries a text answer and, optionally, one short coach
  video. No findings editor and no drills step — the coach editor shows a single "Answer" panel
  for a `QUICK` submission.
- The queue shows quick questions as their own lane or with a badge, so the coach can clear them
  in a batch.
- A monthly quota per membership tier (`BASE` n, `PRO` m), checked when the submission is
  created, returning `409 quota-exceeded` with the date it resets. It counts `QUICK` submissions
  in the player's calendar month, in their timezone. The numbers are the client's call.

**C. A separate "questions" module** with its own table and its own reply entity.

- The cleanest boundary, but it duplicates the video pipeline hooks, the queue, playback
  entitlement and the player's history for something that is structurally a small submission.

## Decision (proposed)

Option B. It fits the model without a new pipeline: one enum column (plus an optional parent
link), a validation branch in submission creation, a quota check, and a trimmed editor path.

## Consequences and effort

- Backend, about 2-3 days: migration (`kind`, `parent_analysis_id`), a `QUICK` branch in
  `POST /api/v1/submissions` (shorter duration, quota), a `QUICK` flag on the queue DTO, an
  `answer` on the analysis (or reuse `summary`), and tests for quota and the answer path.
- Coach app, about 2 days: queue badge/filter and the single-panel answer editor.
- Mobile, about 2 days: a "Quick question" entry next to Submit, a shorter recording cap, the
  remaining quota, and the answer view (text plus optional clip).
- Open questions for the client: quota per tier, maximum clip length, whether the coach may turn
  a quick question into a full breakdown, and whether an unused quota rolls over.
