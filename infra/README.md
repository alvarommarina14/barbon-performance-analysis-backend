# Local infrastructure

```bash
docker compose -f infra/docker-compose.yml up -d
```

| Service  | URL                     | Credentials             |
| -------- | ----------------------- | ----------------------- |
| Postgres | `localhost:5432`        | `courtproof`/`courtproof` |
| MinIO    | http://localhost:9001   | `courtproof`/`courtproof` |
| Mailpit  | http://localhost:8025   | —                       |

`seed/` is mounted into Postgres' entrypoint: any `.sql` dropped there runs the first time
the volume is created. Schema itself belongs in Flyway migrations, not here — this is for
development fixtures only.

Cloudflare Stream has no local emulator. Video work runs against a real development account;
see `backend/src/main/resources/application.yml` for the settings it reads.
