# Court Proof

Video analysis platform for Barbon Performance. A player records themselves on court and
uploads the video, the coach analyses it, and the player gets back an edited feedback video
with findings marked by timestamp.

The architecture, the data model and the phase plan live in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).
The product mockup in [`docs/court-proof-app-mockup-v3.html`](docs/court-proof-app-mockup-v3.html)
is the specification for both UX and copy — open it alongside the app when building a screen.

## Layout

| Path                         | What it is                                              |
| ---------------------------- | ------------------------------------------------------- |
| `backend/`                   | Spring Boot API, Gradle, Flyway migrations               |
| `frontend/apps/mobile/`      | React Native + Expo, the player app                      |
| `frontend/apps/coach/`       | Electron + React + Vite, the coach desktop app           |
| `frontend/packages/core/`    | Design tokens, shared enums, timestamp formatting        |
| `frontend/packages/api-client/` | Generated API types and the fetch wrapper             |
| `infra/`                     | docker-compose for Postgres, MinIO and Mailpit           |
| `docs/`                      | Architecture, ADRs, mockup                               |

## Running it

Prerequisites: Docker, Node 22+, pnpm 11+. The JDK is downloaded by the Gradle toolchain.

```bash
# 1. Infrastructure
docker compose -f infra/docker-compose.yml up -d

# 2. Backend — http://localhost:8080, docs at /swagger-ui.html
cd backend && ./gradlew bootRun

# 3. Frontend workspace
cd frontend && pnpm install
pnpm mobile   # Expo dev server
pnpm coach    # Electron dev window
```

## The API contract

The backend's OpenAPI spec is the source of truth for both clients. After changing a DTO:

```bash
cd frontend && pnpm gen:api    # backend must be running
```

This rewrites `packages/api-client/src/generated/schema.d.ts`. If a client stops compiling
afterwards, the contract changed and the client has to catch up — which is the point.

## Tests

```bash
cd backend && ./gradlew test   # Testcontainers starts a real Postgres
cd frontend && pnpm typecheck
```

When the Docker daemon is not available, point the suite at an already-running Postgres
instead. It must be an empty database the tests may truncate:

```bash
COURT_PROOF_TEST_DB_URL=jdbc:postgresql://localhost:5432/courtproof_test COURT_PROOF_TEST_DB_USER=courtproof COURT_PROOF_TEST_DB_PASSWORD=courtproof   ./gradlew test
```
