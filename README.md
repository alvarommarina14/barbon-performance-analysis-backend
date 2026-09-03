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
