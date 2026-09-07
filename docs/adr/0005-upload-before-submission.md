# ADR 0005 — Create the submission after the upload, not before

Status: accepted (Phase 2)

## Context

The first cut of the upload flow (ADR context, `V3__video.sql`) created both rows up front:
`POST /api/v1/submissions/upload-intent` wrote a `video_asset` **and** a `video_submission`,
then called the provider. The reasoning was that the submission id went into the upload
metadata.

Testing the local provider exposed the cost. The video is uploaded phone → tusd directly, so
if that upload fails or the app is killed, the backend never hears about it and the
`video_submission` is left forever in `SUBMITTED` with an asset stuck in `UPLOADING`. The
player sees an empty "Processing" card that never resolves. The stop-gap — a `DELETE`
endpoint the client calls on failure plus a sweep of stuck submissions — is compensating for
writing a user-visible row before there is anything behind it.

The submission id is not actually needed before the upload: the webhook is matched by
`stream_uid` (via `videoAssets.findByStreamUid`), not by the metadata.

## Decision

**`upload-intent` creates only the `video_asset`. The `video_submission` is created by a
separate `POST /api/v1/submissions` once the upload has landed.**

- `POST /api/v1/submissions/upload-intent` → `{ videoAssetId, uploadUrl, maxDurationSeconds }`.
  The asset is `PENDING_UPLOAD`, stamped with `uploaded_by_player_id` (`V4`), and the upload
  metadata carries `videoAssetId`.
- The client uploads, then `POST /api/v1/submissions`
  (`{ videoAssetId, strokeType, playerNote?, recordedAt? }`). The backend checks the asset is
  a `PLAYER_UPLOAD` **owned by this player** and `READY`, then creates the submission. While
  the webhook is still in flight it returns a retryable `409 video-not-ready`; the client
  polls. A second call for the same asset is `409` ("already has a submission").
- `OrphanedUploadSweeper` now sweeps **orphan assets** — `PLAYER_UPLOAD`, older than
  `analysis.video.orphaned-upload-grace`, with no submission — deleting the row and, best
  effort, the stored bytes (`StreamGateway.discardUpload`). An orphan asset is invisible to
  players and coaches, so this is routine housekeeping, not data loss.
- `DELETE /api/v1/submissions/{id}` stays, but as a real feature: the player retracts a
  submission the coach has not started on.

## Consequences

- **A `video_submission` always has a `READY` video.** No stuck "Processing" cards.
- One extra round trip (`POST /submissions`) and a short window where a `READY` asset has no
  submission. The client shows "Processing…" during it.
- A new column and a new endpoint — the OpenAPI contract changed; `pnpm gen:api` and the
  mobile client caught up.
- Every direct-upload provider (S3 multipart, Cloudflare, Stripe PaymentIntents) creates the
  upload resource speculatively and sweeps orphaned ones; the asset is now that resource.
- The coach feedback upload (`AnalysisService.requestFeedbackUploadIntent`) still attaches
  the asset to the analysis before the upload. Lower stakes — the analysis already exists and
  is the coach's own draft — so it is left as is for now.
