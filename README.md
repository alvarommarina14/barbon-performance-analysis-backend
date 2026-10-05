# Barbon Performance Analysis — backend

Video analysis platform for Barbon Performance. A player records themselves on court and
uploads the video, the coach analyses it, and the player gets back an edited feedback video
with findings marked by timestamp.

This repository holds the Spring Boot API. The two clients live in
`barbon-performance-analysis-frontend`.

The architecture, the data model and the phase plan live in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).
The product mockup is the specification for both UX and copy; it lives in the
`barbon-performance-analysis-frontend` repository at
`docs/barbon-performance-analysis-mockup-v3.html`.

## Layout

| Path     | What it is                                     |
| -------- | ---------------------------------------------- |
| `src/`   | Spring Boot API, Flyway migrations              |
| `infra/` | docker-compose for Postgres, MinIO and Mailpit  |
| `docs/`  | Architecture and ADRs                           |

## Running it

Prerequisites: Docker. The JDK is downloaded by the Gradle toolchain.

```bash
docker compose -f infra/docker-compose.yml up -d
./gradlew bootRun     # http://localhost:8080, docs at /swagger-ui.html
```

Production runs on Railway with Cloudflare Stream and R2; the steps are in
[`docs/deploy.md`](docs/deploy.md).

## Running the video loop locally

Cloudflare Stream is not free, so development uses a **local video provider** — `tusd` +
MinIO, both in `infra/docker-compose.yml` — selected by `analysis.video.provider=local`.
Production keeps using Cloudflare. Trade-offs are in
[`docs/adr/0004-local-video-provider.md`](docs/adr/0004-local-video-provider.md).

```bash
cp infra/.env.example infra/.env
docker compose --env-file infra/.env -f infra/docker-compose.yml up -d
./gradlew bootRun --args='--spring.profiles.active=local'
```

The `local` profile sets `analysis.video.provider=local`. On startup the log prints which
provider is active — look for `Video provider: LOCAL`. In IntelliJ, set the run
configuration's active profile to `local` (env vars are not picked up otherwise).

The backend must listen on **:8080** — that is where tusd (in the container) sends its
post-finish hook. If tusd logs `connection refused` for the hook, either the backend is not
on :8080 / not on the `local` profile, or set `TUSD_HOOK_URL` in `infra/.env` to the
machine's LAN address.

**End-to-end with a phone (Expo Go) on the same Wi-Fi** works with no extra configuration:
the phone reaches the backend at `http://<lan-ip>:8080`, and while the configured
`analysis.video.local.*` hosts are `localhost` the backend hands back upload and playback
URLs built from that same `<lan-ip>` (keeping the tusd/MinIO ports). Set
`ANALYSIS_VIDEO_PUBLIC_URL` / `ANALYSIS_VIDEO_S3_PUBLIC_ENDPOINT` only to pin a specific
address.

## The API contract

The OpenAPI spec served at `/v3/api-docs` is the source of truth for both clients. After changing
a DTO, run `pnpm gen:api` in the frontend repository with this backend running. If a client stops
compiling afterwards, the contract changed and the client has to catch up — which is the point.

## Tests

```bash
./gradlew test   # Testcontainers starts a real Postgres
```

When the Docker daemon is not available, point the suite at an already-running Postgres
instead. It must be an empty database the tests may truncate:

```bash
ANALYSIS_TEST_DB_URL=jdbc:postgresql://localhost:5432/bpa_test ANALYSIS_TEST_DB_USER=bpa ANALYSIS_TEST_DB_PASSWORD=bpa   ./gradlew test
```
