# ADR 0001 — Pinned toolchain versions

Status: accepted (Phase 0)

## Context

`docs/ARCHITECTURE.md` says versions are pinned in Phase 0 "against what is currently stable,
not copied from another project". These are the versions that were current when the repository
was set up, and the reasoning behind the ones that were not simply "the latest".

## Decision

| Component      | Version   | Note                                                            |
| -------------- | --------- | --------------------------------------------------------------- |
| Java           | 21 (LTS)  | Declared as a Gradle toolchain, not as "whatever is installed"   |
| Gradle         | 9.2.0     | Wrapper, already present                                         |
| Spring Boot    | 4.1.1     | Latest stable; brings Spring Framework 7 and Spring Security 7   |
| Flyway         | Boot-managed | Pinning it separately only invites version skew               |
| Testcontainers | 2.0.5     | Boot-managed. Note the 2.x artifact ids: `testcontainers-postgresql` |
| MapStruct      | 1.6.3     | Latest stable; 1.7 is still in beta                              |
| springdoc      | 3.1.0     | The 3.x line targets Boot 4; 2.x targets Boot 3                  |
| jjwt           | 0.13.0    |                                                                  |
| AWS SDK v2     | 2.54.5    | S3 client only, pointed at R2                                    |
| Node           | 22+       |                                                                  |
| pnpm           | 11.24.0   | Recorded in `packageManager`                                     |
| TypeScript     | 6.0.3     | Follows what the Expo SDK 57 template pins, not the newest release |
| Expo SDK       | 57        | Package versions come from `expo install`, which resolves SDK-compatible ranges |
| Electron       | 44        | With electron-vite 5 and Vite 8                                  |

### Java 21 rather than the newest JDK

No LTS JDK is installed on the development machine, and the ones that are (20, 23, 24) are all
past end of life. Rather than build against whatever happens to be on `PATH`, the build declares
a Java 21 toolchain and `foojay-resolver-convention` downloads it. Same JDK on every machine and
in CI, and `JAVA_HOME` stops mattering.

Java 25 is also LTS and newer, but MapStruct's annotation processor and the rest of the stack
have more mileage on 21. Revisit when there is a reason to.

### TypeScript 6, not 7

TypeScript 7 (the Go port) is the current stable release, but the Expo SDK 57 template pins
`~6.0.3`. With a single hoisted `node_modules` for the whole workspace, running two TypeScript
majors is asking for trouble, so the workspace follows Expo. Move when Expo does.

### pnpm with `node-linker=hoisted`

React Native and Metro resolve modules by walking `node_modules`, which pnpm's symlinked layout
breaks. Hoisting is the supported configuration for Expo inside a pnpm workspace, and it is set
in `frontend/.npmrc`.

## Consequences

Version bumps are a deliberate edit to `backend/build.gradle` or a `package.json`, never a
side effect of an install. Anything the Spring Boot BOM already manages is left unpinned so
that upgrading Boot upgrades the set coherently.
