# ADR 0004 — A local video provider for development

Status: accepted (Phase 2)

## Context

Phase 2 is the video loop, and it cannot be demonstrated end to end without a real video
provider. The only adapter written is `CloudflareStreamGateway`, and Cloudflare Stream has
no free tier (US$5/mo minimum) and is not enabled on the development account — every Stream
endpoint returns `"Cloudflare Stream not enabled"`.

We need to show the full experience — a player uploads a stroke from a phone, the coach
reviews it in the desktop app, marks findings, publishes, and the player watches the
feedback video — running entirely on a laptop and a phone on the same Wi-Fi, at no cost.

The seam already exists. `video/domain/StreamGateway` and `video/domain/StreamWebhooks` are
ports; `ADR 0003` already anticipated a second provider ("if a third asset kind needs
playback … extract a shared helper").

## Decision

**Add a second adapter behind each port, selected by `analysis.video.provider`
(`cloudflare` | `local`). `CloudflareStreamGateway` is untouched and stays the production
path.**

Local mode is `tusd` + MinIO, both already in `infra/docker-compose.yml` (MinIO was there
for R2; tusd is new):

- **Upload** — `LocalStreamGateway.createDirectUpload` does the tus creation `POST` against
  tusd exactly as the Cloudflare adapter does against Stream, and returns tusd's one-time
  URL. tusd runs with `-behind-proxy`; the backend sends `X-Forwarded-Host` /
  `X-Forwarded-Proto` built from `analysis.video.local.public-base-url` so the advertised
  URL is reachable from the phone, not from inside the container. The `stream_uid` is the
  MinIO object key (tusd's upload id with the `+<multipartId>` suffix stripped).
- **Ready signal** — tusd's `post-finish` HTTP hook calls `POST /api/v1/webhooks/tusd`.
  `LocalStreamWebhooks` implements the same `StreamWebhooks` port, so `StreamWebhookService`
  is unchanged. tusd cannot attach a static header, so the shared secret rides in the hook
  URL's query string; the adapter compares it in constant time. There is **no transcode**,
  so `duration` and `thumbnail` are unknown locally — the columns are nullable and the
  clients already tolerate it.
- **Playback and download** — `signPlayback` / `enableDownload` return a pre-signed MinIO
  `GET` URL (AWS SDK v2 `S3Presigner`, which signs offline). No HLS: the object is served as
  the MP4 it was uploaded as. `signPlayback` now returns a `StreamGateway.Playback` record
  instead of a bare token, which also removes the duplicated `videodelivery.net` URL
  literal noted as a wart in `ADR 0003`.

The webhook endpoint keeps its Cloudflare-specific path (`/webhooks/cloudflare-stream`); the
local one is a separate `/webhooks/tusd`. Only the adapter for the configured provider is a
bean, so a call to the wrong endpoint is simply rejected.

## Consequences

- The whole loop runs for free on a laptop + phone. `ArchitectureTest` stays green: both new
  classes are package-private adapters under `infrastructure/gateway/`.
- Local playback has no adaptive bitrate and no poster/duration metadata. Acceptable for
  development and demos; not a production option.
- The upload and playback URLs must be reachable from wherever the client runs. When the
  configured `analysis.video.local.*` host is loopback, `LocalStreamGateway` substitutes the
  inbound request's own host (keeping the configured port), so a phone that reached the
  backend at `http://<lan-ip>:8080` gets `http://<lan-ip>:1080` / `:9000` back with no
  configuration. `*_PUBLIC_URL` / `*_PUBLIC_ENDPOINT` still pin a fixed address if needed.
  The MinIO pre-signer is per-resolved-endpoint because the host is part of the signature.
- Migrating to Cloudflare later is setting `analysis.video.provider=cloudflare` and
  supplying the account credentials. The known integration checks against a real Stream
  account (the `Upload-Metadata` key format, the real webhook payload and HMAC header, the
  `/downloads` shape) are still outstanding and tracked separately.
