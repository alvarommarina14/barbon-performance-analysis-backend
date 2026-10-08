# Deploying the backend

Production runs on **Railway** (the Spring Boot app as a Docker image plus a managed Postgres),
with **Cloudflare Stream** for video and **Cloudflare R2** for PDFs. The app starts with the
`prod` profile (`src/main/resources/application-prod.yml`), which has no default for any secret:
a missing variable stops the app at startup rather than letting it run on development values.

Order matters: Cloudflare first (Railway needs its values), then Railway, then the Stream webhook
(Cloudflare needs Railway's public URL), then the coach account.

## 1. Cloudflare

All in the same Cloudflare account. Note the **Account ID** (dashboard home, right column).

**Stream**
1. Subscribe to Stream (Stream → Plans). The smallest plan covers informal testing.
2. Stream → any video's embed code shows the delivery host, `customer-<code>.cloudflarestream.com`.
   That host with `https://` in front is `CLOUDFLARE_DELIVERY_BASE_URL`.
3. My Profile → API Tokens → Create Token → custom token with **Account · Stream · Edit**,
   limited to this account. That is `CLOUDFLARE_API_TOKEN`.

**R2**
1. R2 → Create bucket, e.g. `barbon-performance-analysis`. That is `R2_BUCKET`.
2. R2 → Manage R2 API Tokens → Create API token with **Object Read & Write** on that bucket.
   It shows an **Access Key ID**, a **Secret Access Key** and the S3 endpoint
   `https://<account-id>.r2.cloudflarestorage.com` (`R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`,
   `R2_ENDPOINT`). The secret is shown once.

The bucket needs no CORS rule: PDFs are uploaded from the coach app's main process, not a browser.

## 2. Railway

1. New Project → Deploy from GitHub repo → this repository, branch `main`. Railway picks up
   `railway.json` and builds the `Dockerfile`. The first build fails on the missing variables;
   that is expected.
2. In the same project: New → Database → PostgreSQL.
3. Backend service → Settings → Networking → Generate Domain. Call it `https://<api-host>`; the
   program PDF needs it before the first successful start.
4. Backend service → Variables:

   | Variable | Value |
   |---|---|
   | `PGHOST` | `${{Postgres.PGHOST}}` |
   | `PGPORT` | `${{Postgres.PGPORT}}` |
   | `PGDATABASE` | `${{Postgres.PGDATABASE}}` |
   | `PGUSER` | `${{Postgres.PGUSER}}` |
   | `PGPASSWORD` | `${{Postgres.PGPASSWORD}}` |
   | `JWT_SECRET` | output of `openssl rand -base64 48` |
   | `CLOUDFLARE_ACCOUNT_ID` | from step 1 |
   | `CLOUDFLARE_API_TOKEN` | from step 1 |
   | `CLOUDFLARE_DELIVERY_BASE_URL` | from step 1 |
   | `CLOUDFLARE_WEBHOOK_SECRET` | `pending` for now — replaced in section 3 |
   | `R2_ENDPOINT` | from step 1 |
   | `R2_BUCKET` | from step 1 |
   | `R2_ACCESS_KEY_ID` | from step 1 |
   | `R2_SECRET_ACCESS_KEY` | from step 1 |
   | `ANALYSIS_PUBLIC_BASE_URL` | `https://<api-host>` from item 3 above, no trailing slash |
   | `PROGRAM_LINK_SECRET` | another `openssl rand -base64 48`; never rotate it, it signs the links in PDFs already handed out |

   `PORT` is set by Railway; do not add it.
5. Wait for the deploy; `https://<api-host>/actuator/health` must answer `{"status":"UP"}`.
   Flyway creates the schema and the organisation on the first start.

## 3. Stream webhook

Cloudflare tells the backend when a video is ready. Register the endpoint once:

```bash
curl -X PUT "https://api.cloudflare.com/client/v4/accounts/$CLOUDFLARE_ACCOUNT_ID/stream/webhook" \
  -H "Authorization: Bearer $CLOUDFLARE_API_TOKEN" \
  -H "Content-Type: application/json" \
  --data '{"notificationUrl":"https://<api-host>/api/v1/webhooks/cloudflare-stream"}'
```

The response carries `result.secret`. Put it in `CLOUDFLARE_WEBHOOK_SECRET` on Railway; Railway
redeploys on its own.

## 4. Accounts

- **Coach — do this right after the first deploy.** `POST /api/v1/auth/register-coach` is open
  and the organisation accepts exactly one coach (ADR 0002), so whoever registers first becomes
  the coach. Install the coach app built against `https://<api-host>` (see the frontend
  repository's README) and use "Create your account".
- **Players** register from the mobile app and are assigned to the coach automatically.
  Registration is open to anyone who has the URL.
- There is no password reset yet. A forgotten password means updating `app_user.password_hash`
  by hand (a BCrypt hash) from Railway's Postgres console.

## Updating

Every push to `main` redeploys. Migrations run on startup; never edit one that has been applied.
