# ADR 0003 — Playback entitlement without a module cycle

Status: accepted (Phase 2)

## Context

Phase 2 adds the `analysis` module. An analysis carries a feedback video — a `video_asset` of
`kind = COACH_FEEDBACK` — that the player watches once the analysis is published.

`analysis` already depends on `video`: the feedback upload intent
(`POST /api/v1/analyses/{id}/feedback-upload-intent`) calls `video.domain.StreamGateway` and
saves a `video.domain.VideoAsset`. That is an `analysis → video` module edge.

`video`'s existing `PlaybackService` (`GET /api/v1/videos/{assetId}/playback`) decides
entitlement by looking up the `video_submission` an asset belongs to (player owns the submission,
or the coach owns the player). A `COACH_FEEDBACK` asset has **no** submission — its context is
the `analysis` row that points at it — so `PlaybackService` cannot authorise it today.

The obvious fix — teach `PlaybackService` to also look up an analysis by
`feedback_video_asset_id` — would add a `video → analysis` edge. Combined with the existing
`analysis → video` edge that is a **cycle between module slices**, which `ArchitectureTest`
(`modulesAreFreeOfCycles`) fails the build on.

## Decision

**Each module owns the playback entitlement for the assets whose context it owns, and exposes
its own playback endpoint.**

- `GET /api/v1/videos/{assetId}/playback` (video) — the player upload. Unchanged: player owns the
  submission, or the coach owns the submission's player.
- `GET /api/v1/analyses/{analysisId}/playback` (analysis) — the feedback video.
  `AnalysisPlaybackService` resolves the analysis, checks entitlement (player: the analysis is
  `PUBLISHED` and theirs; coach: the analysis is theirs, any status, so a draft can be
  previewed; admin: always), then reuses `video.domain.StreamGateway.signPlayback` to mint the
  token. The response DTO (`AnalysisDtos.PlaybackView`) lives in `analysis`, not `video`.

A missing analysis returns `403`, not `404`, exactly as `video.PlaybackService` does, so the id
space cannot be probed.

The client already knows which kind of asset it is showing (a submission vs. an analysis), so it
calls the matching endpoint — no dispatch logic is lost.

## Consequences

- No module cycle; `ArchitectureTest` stays green with `analysis → video` as the only edge.
- The signed-URL shape (`https://videodelivery.net/<token>/manifest/video.m3u8`) is now written
  in two places (`video.PlaybackService`, `analysis.AnalysisPlaybackService`). It is three lines
  and unlikely to drift; if a third asset kind needs playback (library drills, protocols in
  Phase 3/4) this is the point to extract a shared `video.domain` helper or SPI.
- The rejected alternative — a `video.domain` entitlement SPI that other modules contribute
  `AssetEntitlement` beans to — keeps one endpoint but adds a registry and an indirection for a
  rule that is one `switch` per module. Revisit only if the number of playable asset kinds grows.
