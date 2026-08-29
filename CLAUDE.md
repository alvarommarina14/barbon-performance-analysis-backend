# Court Proof — working agreement

Video analysis platform for Barbon Performance. The architecture, the data model and the phase
plan live in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md); the product mockup in
[`docs/court-proof-app-mockup-v3.html`](docs/court-proof-app-mockup-v3.html) is the specification
for both UX and copy. Read them before building a screen or an endpoint.

## Comments and documentation

**No JavaDoc.** Never write a `/** ... */` block anywhere in this repository — not on classes,
records, enums, interfaces, methods, fields, constants or tests — and never write `@param`,
`@return`, `@throws`, `@see` or `{@link}` tags. Do not reintroduce one into a file that has none,
and do not "helpfully" document a method you are only passing through. TSDoc `/** */` blocks in
the frontend are covered by the same rule. Generated files are exempt — nobody edits
`packages/api-client/src/generated/`, `pnpm gen:api` rewrites it.

A `//` comment is allowed, but only when it records something the code genuinely cannot say: a
decision with a non-obvious trade-off, a workaround for external behaviour, a constraint that
would look like a mistake to the next reader. A comment that restates the signature, names the
obvious, or narrates the next three lines is noise — delete it instead of writing it.

Names, small methods and tests carry the explanation. The long-form "why" belongs in
`docs/ARCHITECTURE.md`, in an ADR under `docs/adr/`, or in the commit message.

## Language

Everything that lives in the repository is in **English**: code, identifiers, file names,
comments, commit messages, branch names, docs, and the output of development scripts.

The single exception is **end-user product copy**, which follows the product's language.

## Git

- Conventional Commits: `<type>(<scope>): <imperative, lowercase, no trailing period>`.
  Types: `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `build`, `ci`, `chore`, `style`.
- Incremental commits — one logical change each, and every commit compiles on its own.
- Never commit directly to `main`. Branch per unit of work: `feat/<slug>`, `fix/<slug>`,
  `chore/<slug>`, `refactor/<slug>`. Merge with `--no-ff`.
- **Never push, never open a pull request.** Commits stay local; publishing is Alvaro's call.
- No `Co-Authored-By` trailers and no tool attribution in commit messages or PR descriptions.

## Backend

Spring Boot 4.1 on Java 21, Gradle, Postgres with Flyway, Testcontainers for integration tests.

**Ports and adapters, module on the outside, layers inside.** A new feature is a new package
under `com.barbonperformance.courtproof` containing `controller/`, `dto/`, `application/`,
`domain/`, `persistence/`, and `gateway/` when it talks to a third party. `docs/ARCHITECTURE.md`
has the full tree and the reasoning; the short version:

- `domain/` holds the entities and the **ports** — interfaces named for what the module needs
  (`AppUsers`, `RefreshTokens`, `StreamGateway`), owned by the domain, not by the technology.
  It depends on nothing around it.
- `persistence/` and `gateway/` hold the adapters, and they are **package-private**. Callers get
  the port, never the implementation.
- `application/` orchestrates ports and owns `@Transactional`. It never touches an adapter.
- `controller/` is HTTP in and DTO out. No persistence.
- **Spring Data types never leave `persistence/`.** A `JpaRepository` is an implementation detail.
- Entities keep their JPA annotations — deliberate; the port is what buys the decoupling, not a
  hand-mapped domain model.

`ArchitectureTest` enforces every one of those rules and fails the build. When it goes red, fix
the placement rather than the rule.

- Every table carries `org_id`, and every query is scoped by it. There is one organisation today;
  reads that ignore the column are bugs waiting for the second one.
- Schema changes are Flyway migrations. Never edit a migration that has already been applied.
- Errors reach the client as RFC 7807 problem details through `ApiExceptionHandler`.
- Constructor injection, no field injection. No Lombok.
- Role checks go on the handler with `@PreAuthorize`, not in the filter chain.

## The API contract

The backend's OpenAPI spec is the source of truth for both clients. After changing a DTO, run
`pnpm gen:api` from `frontend/` with the backend running. If a client stops compiling afterwards,
the contract changed and the client has to catch up — that is the point, not a problem to route
around.

## Tests

```bash
cd backend && ./gradlew test        # Testcontainers starts a real Postgres
cd frontend && pnpm typecheck
```

Docker does not run on this machine, so the suite also honours `COURT_PROOF_TEST_DB_URL` (plus
`_USER` and `_PASSWORD`) to run against an already-running Postgres. It must be an empty database
the tests may truncate. See the README.
