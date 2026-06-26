# SharePay — Full Stack (Release 1.0 MVP)

Event expense-settlement platform. Groups ("workspaces") run "events", record expenses with
flexible splits, sponsorships, refunds and adjustments, and the system computes per-member
balances and an **optimized set of settlement transfers** ("who pays whom").

This repository contains the **Spring Boot backend** and the **React/TypeScript frontend**
(`frontend/`).

## Tech stack

Java 17 · Spring Boot 3.3 · Spring Web / Data JPA / Security · JWT (jjwt, HS256) ·
PostgreSQL (prod) + H2 (dev/test) · Flyway · springdoc-openapi (Swagger UI) ·
Apache POI (Excel export) · Maven · Docker.

> The spec targets **Java 21**; this build targets **Java 17 LTS** because that is the JDK
> available locally (Java 17 is Spring Boot 3.3's baseline). To match the spec, install a JDK 21
> and change `<java.version>` in `pom.xml` to `21`. Bytecode compiled at 17 also runs on 21,
> and the Docker image already builds with a JDK 21 toolchain.

## Running

### Option A — local dev (H2, no Docker needed)

```bash
./mvnw spring-boot:run
```

Starts on the `dev` profile with an in-memory H2 database and **seeds demo data**:

```
email:    huy@sharepay.dev
password: password123
Workspace "Huy & Friends" -> Event "Da Nang Trip 2026" (id 1)
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 console:  http://localhost:8080/h2-console  (JDBC URL `jdbc:h2:mem:sharepay`, user `sa`, no password)

### Option B — Docker (full stack: nginx + frontend + backend + PostgreSQL)

```bash
docker compose up --build
```

Brings up PostgreSQL, the backend (`docker` profile, Flyway migrations + Hibernate `validate`),
and the **frontend served by nginx** which proxies `/api` to the backend. Open the app at
**http://localhost:5173**. (Demo data is **not** seeded outside the `dev` profile — register a
user via the UI.)

## Deploying

### AWS — single EC2 + docker-compose

Runs the whole stack (Postgres + backend + nginx-served frontend) on one EC2 instance using
`docker-compose.prod.yml`, which exposes only port 80 and keeps the DB/backend internal.
Step-by-step AWS CLI runbook: **[deploy/aws/README.md](deploy/aws/README.md)**. In short:

```bash
# on a fresh Amazon Linux 2023 instance, with the repo checked out:
bash deploy/aws/setup-ec2.sh            # Docker + Compose + swap (then re-login)
cp .env.prod.example .env && nano .env  # set DB_PASSWORD + JWT_SECRET
bash deploy/aws/deploy.sh               # build + start; app on http://<public-dns>
```

### Netlify frontend + container backend

Netlify hosts only the **static frontend**. The **backend + PostgreSQL** must run on a
container host. The repo includes `netlify.toml` (build + SPA fallback + an `/api/*` proxy).

**1. Deploy the backend** (Render / Railway / Fly.io / Koyeb — all build from the `Dockerfile`):
- Create a managed PostgreSQL instance.
- Deploy this repo's `Dockerfile`. Set env vars:
  `SPRING_PROFILES_ACTIVE=docker`, `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`,
  and a strong `JWT_SECRET` (≥ 32 chars). Note the public URL, e.g. `https://sharepay-api.onrender.com`.

**2. Point the proxy at the backend**: in `netlify.toml`, replace `YOUR-BACKEND-HOST` in the
`/api/*` redirect with that host. (This proxies API calls server-side, so the browser stays
same-origin and no CORS config is needed.)

**3. Deploy the frontend to Netlify** (Git-based, recommended):
- Push this repo to GitHub/GitLab, then in Netlify: **Add new site → Import from Git** → pick the repo.
- Netlify reads `netlify.toml` automatically (build command, publish dir `frontend/dist`, redirects).
  Deploy.

Alternative to the proxy: set `VITE_API_URL=https://YOUR-BACKEND-HOST/api` as a Netlify build env
var (calls the backend directly), and set `CORS_ALLOWED_ORIGINS=https://your-site.netlify.app` on
the backend so it accepts the browser's cross-origin requests.

## Frontend (`frontend/`)

React 18 + TypeScript + Vite + Material UI + MUI X Charts + TanStack React Query. Covers the full
10-screen flow: login/register, workspaces, events, event dashboard (donut + totals), members,
transactions list, add/edit transaction (multi-payer, split methods, sponsors), transaction
detail (+ receipt upload), summary, and settlement (+ Excel/CSV export).

```bash
cd frontend
npm install
npm run dev      # http://localhost:5173, proxies /api -> http://localhost:8080
npm run build    # type-check (tsc) + production bundle
```

Run the backend first (Option A for the seeded H2 demo, then log in with
`huy@sharepay.dev` / `password123`).

### Tests

```bash
./mvnw test
```

Covers the settlement optimizer, split-rounding edge cases, the **ledger zero-sum invariant**
across mixed expense/sponsor/refund/adjustment flows, and a full HTTP auth flow.

## Authenticating

```bash
# Register (or use the seeded demo account in dev)
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"you@example.com","password":"password123","displayName":"You"}'

# Login -> returns { "token": "...", "user": {...} }
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"you@example.com","password":"password123"}'
```

Send the token as `Authorization: Bearer <token>` on every other request.

## Key API endpoints

| Area | Endpoint |
|------|----------|
| Auth | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` |
| Workspaces | `GET/POST /api/workspaces`, `GET/PUT/DELETE /api/workspaces/{id}`, `.../members` |
| Events | `GET/POST /api/workspaces/{wid}/events`, `GET/PUT/DELETE /api/events/{id}`, `PATCH /api/events/{id}/status`, `POST /api/events/{id}/duplicate` |
| Event members | `GET/POST /api/events/{id}/members`, `PUT/DELETE .../{memberId}` |
| Transactions | `GET /api/events/{id}/transactions`, `POST .../expenses`, `.../refunds`, `.../adjustments`, `PUT .../expenses/{txId}`, `DELETE .../{txId}`, `POST .../{txId}/receipts` |
| Analytics | `GET /api/events/{id}/dashboard`, `/summary`, `/members-summary`, `/categories` |
| Settlement | `GET /api/events/{id}/settlement`, `POST .../settle` |
| Export | `GET /api/events/{id}/export/excel`, `/export/csv` |

## Accounting model (how balances are computed)

The system uses an **immutable, append-only double-entry ledger** (`ledger_entries`). No balance
is ever stored — balances are always derived by summing entries:

```
balance = paid - share - sponsored + refunds + adjustments
```

where `refunds` and `adjustments` are the net (credit − debit) of those entry types. Every
transaction posts balanced credits and debits, so **the sum of all member balances in an event is
always zero**, which is what makes settlement solvable.

Posting per transaction type (see `LedgerService`):

- **Expense** (amount `A`, sponsored total `S`, net `A − S`): payers → `PAYMENT_CREDIT`
  (must sum to `A`); participants → `SHARE_DEBIT` of their split of the net; sponsors →
  `SPONSOR_DEBIT` of their pledged amount (the sponsor funds it as a gift, reducing everyone's share).
- **Refund** (amount `R`): the receiver → `REFUND_DEBIT R` (holds returned group cash); beneficiaries
  → `REFUND_CREDIT` summing to `R` (their net cost drops).
- **Adjustment** (amount `X`): `ADJUSTMENT_CREDIT X` to one member, `ADJUSTMENT_DEBIT X` to another.

**Split methods** (`split` package): `EQUAL`, `EXACT`, `PERCENTAGE`, `WEIGHT`. Money is split in
the smallest currency unit and the rounding remainder is distributed so no money is lost
(important for zero-decimal currencies like VND).

**Settlement** (`SettlementOptimizer`): a greedy minimum-cash-flow algorithm matches the largest
debtor against the largest creditor repeatedly, producing at most `n − 1` transfers.

## Permissions

Workspace roles gate actions: `OWNER` (everything incl. manage workspace/members, delete events),
`ADMIN` (manage event, add transactions, generate settlement), `MEMBER` (add transactions, view),
`VIEWER` (read-only). Enforced in `PermissionService`.

## Not in this pass (follow-ups)

Frontend (React/MUI), Google OAuth, **PDF export** (Excel/CSV are implemented), OCR, QR payment,
notifications, multi-currency conversion — all Phase 2/3 or the deferred frontend pass.
