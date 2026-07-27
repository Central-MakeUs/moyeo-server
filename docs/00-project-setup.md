# Project Setup Policy

> Last reviewed: 2026-07-26
> Review trigger: 기술 스택, MVP 범위, 배포 방식, Codex 작업 규칙, 도메인 정책 변경 시

## Project Goal

- Moyeo is a product intended for continued development and operation.
- The current six-week MVP is the initial delivery milestone, not the expected
  lifetime of the project.
- Core MVP delivery is the current priority.
- The server must remain deployable and maintainable.
- Avoid premature infrastructure and speculative abstractions.
- Avoid disposable MVP shortcuts that create unnecessary schema, API, migration,
  or operational debt.
- Prefer simple designs that can evolve incrementally when requirements are
  confirmed.

## Project Lifecycle Context

The first milestone is a six-week MVP phase. After the MVP, product requirements
are expected to be validated and evolved through continued development and
operation.

This context should not be used as a reason to introduce enterprise-scale,
high-traffic, large-system, or microservice-readiness assumptions before those
requirements are confirmed. It should also not be used as a reason to create
undocumented temporary behavior or disposable implementations that conflict with
confirmed product direction.

## MVP Priority

The following is a candidate core flow for later implementation, not today's
implementation scope.

```text
Create appointment meeting
Share link
Collect participant responses
Close voting
View results
Finalize decision
```

## Current Scope

- Basic server runtime
- Health check
- Swagger/OpenAPI
- Actuator
- Local profile
- H2 local datasource
- MySQL dev/prod datasource configuration
- GitHub Actions CI/CD
- Common error response base
- Member/social-account base entities
- Social login and Access JWT issue flow
- First milestone meeting creation, invite-code lookup, and guest participation flow
- AWS dev deployment

## Tech Decisions

### Java 21

- Use an LTS release for stability and long-term maintainability in a new
  project.
- Java 8/11 remain meaningful for legacy maintenance but offer little reason for
  selection in a new MVP.

### Spring Boot 3.5.x

- Prioritize stability, reference accessibility, and ecosystem compatibility for
  a six-week MVP and team collaboration.

### JPA

- Appointment meetings, participants, votes, responses, and results are expected to
  have clear relationships, making a relational model and ORM a suitable
  candidate.
- Current implemented entities are limited to member/login base structures.

### MySQL

- Use MySQL as the candidate database for real dev/prod environments.
- Dev server currently uses MySQL 8.4 running in Docker Compose on the EC2 dev
  instance to reduce early MVP infrastructure cost.
- Dev server temporarily uses Hibernate `ddl-auto=update` while the MVP schema
  is still changing quickly. Revisit this before real user data matters and move
  to explicit migrations or `validate`.
- Before deploying social login to an existing production database, back up the
  target database and apply `scripts/db/2026-07-24-social-login.sql` so
  `users.nickname` accepts the pending-onboarding null value. Production keeps
  `ddl-auto=none`; the application does not run this SQL automatically.
- Before deploying account withdrawal to production, apply
  `scripts/db/2026-07-25-meeting-cover-cleanup.sql`. The cleanup task table keeps
  failed S3 cover deletions retryable after a process restart.
- Before deploying re-login-free Apple withdrawal to an existing database,
  apply `scripts/db/2026-07-26-social-refresh-token.sql`. Existing Apple users
  populate the new nullable ciphertext column on their next successful login.
- Account-withdrawal cover cleanup retries every five minutes by default.
  `MEETING_COVER_CLEANUP_RETRY_DELAY` may override the Spring duration value.
- Hibernate `ddl-auto=update` does not remove tables for deleted entities. The
  former `login_accounts` table may therefore remain physically in an existing
  dev database after social-only authentication is deployed, although the
  application no longer reads or writes it. Remove it only through a reviewed,
  backed-up one-time database operation.
- Amazon RDS MySQL is not the current default dev database. Keep any remaining
  RDS notes as legacy/reference only.

### H2

- Use only for local development and tests.
- Do not use H2 as the production database.

### Caddy

- Use one Caddy container as the dev HTTPS reverse proxy in the existing EC2
  Docker Compose deployment.
- Use `3-35-119-70.sslip.io` as the temporary dev hostname backed by the current
  Elastic IP.
- Let Caddy manage ACME certificate issuance and renewal automatically; keep
  public ports `80` and `443` reachable for this lifecycle.
- In the `dev` Spring profile, use `server.forward-headers-strategy=framework`
  so requests forwarded by Caddy retain their public HTTPS scheme and
  Swagger/OpenAPI generates HTTPS server URLs.
- Keep Caddy certificate state in named Docker volumes so application
  redeployments do not discard issued certificates. Do not delete
  `moyeo-caddy-data` or `moyeo-caddy-config` during ordinary deployments.
- Monitor the public certificate once per day from GitHub Actions. Fail the
  monitor when the TLS handshake or hostname verification fails, or when fewer
  than 21 days remain before expiration. Keep this check external so it adds no
  resident process or memory usage to the EC2 instance.
- Keep this setup limited to the current single-instance dev environment.

## Currently Excluded

### Domain Logic

- Appointment-meeting domain logic beyond the first milestone meeting/invite/guest
  participation base
- Voting domain logic
- Participant domain logic
- Result domain logic

Do not implement domain behavior until a human-defined policy exists.

### Redis/Kafka/WebSocket/NoSQL

- Current MVP data appears relational.
- Traffic, real-time, and event-processing requirements are not yet clear for the
  six-week MVP.
- Revisit after operational needs are validated.

### Nginx/Blue-Green/Zero-downtime Deployment

- Consider operational needs, but do not implement them in the current dev setup.
- Revisit before public launch or operational hardening.

### MCP/Sub-agents/Complex Hooks

- Codex working rules, documentation, and CI/CD are sufficient for now.
- Revisit only when a concrete need appears.

## Policy References

- API and error contract: `docs/policies/API_POLICY.md`
- Authentication and security: `docs/policies/AUTH_POLICY.md`
- Meeting and participation domain:
  `docs/policies/MEETING_PARTICIPATION_POLICY.md`
- AI code review: `docs/ai/CODE_REVIEW.md`

## AI-assisted Development Policy

- Codex operates inside human-defined boundaries.
- Humans own domain, product, and technical policy decisions.
- AI-generated changes require human review.
- Detailed review behavior is defined in `docs/ai/CODE_REVIEW.md`.
- Codex may assist with implementation, boilerplate, test drafts,
  documentation drafts, Swagger/OpenAPI descriptions, and simple refactoring
  within the requested scope.

The development harness includes GitHub Actions CI/CD, Swagger/OpenAPI, the
current RFC 9457-based error response policy, and documented working rules.

## Deployment Policy

- Use Docker for a repeatable dev deployment artifact.
- Use AWS EC2 as the first dev deployment target.
- Use MySQL 8.4 in Docker Compose on the EC2 dev instance as the current dev
  database.
- Use temporary Hibernate schema update only for the dev profile while MVP schema
  churn is high; do not use it as the production migration strategy.
- Keep the dev MySQL container private to the EC2 Docker network; do not expose
  port `3306` publicly.
- Binding MySQL to `127.0.0.1:3306` on EC2 is allowed for developer DBeaver
  access through SSH tunneling only.
- RDS is legacy/reference only for the current dev setup and may be revisited
  later if managed database reliability becomes more important than early cost
  control.
- Use Amazon ECR for private Docker image storage.
- Use GitHub Actions for build, test, image push, and EC2 deployment automation.
- Use `ohujj/MOYEO` as the sole dev deployment source. Keep the deployment
  workflow file mirrored to `Central-MakeUs/moyeo-server`, but skip its deploy
  job there so a mirrored push cannot deploy the same EC2 instance twice.
- Prefer AWS Systems Manager Run Command over opening SSH to GitHub Actions
  runners.
- Keep EC2 runtime secrets in a server-side `.env` file or managed secret
  storage instead of passing them through deployment commands.
- Write `dev` and `prod` application logs to `/app/logs/moyeo.log` and
  `moyeo-error.log`; roll them over daily and at 25 MB per file, retaining up
  to 30 days and 256 MB total (192 MB general, 64 MB exception logs).
- Docker Compose persists the container log directory through its `LOG_DIR`
  host-directory mount, which defaults to `./logs`.
- Generate a server-owned trace ID for each HTTP request, return it through
  `X-Trace-Id`, and include it in application and exception logs.
- Keep dev/prod secrets in GitHub Secrets or AWS-managed secret storage, not in
  repository files.
- Serve the dev API through Caddy at `https://3-35-119-70.sslip.io`; keep ports
  `80` and `443` public for certificate issuance, HTTP-to-HTTPS redirection, and
  HTTPS traffic.
- Keep dev API port `8080` temporarily public while the frontend application
  migrates from the former direct HTTP endpoint.
- Apple login has been verified end to end from the dev Vercel origin through
  `https://3-35-119-70.sslip.io`, including browser CORS preflight, Apple code
  exchange, existing-user lookup, onboarding state, and Moyeo Access JWT
  issuance.
- TODO: After the frontend application is wired to and deployed with the HTTPS
  API base URL, remove public security-group access to `8080` and stop
  publishing the application container port to the public host.
- Keep SSH port `22` restricted to the developer IP.
- Keep MySQL port `3306` private and accessible only from the EC2 application
  path.
- Keep zero-downtime deployment, blue/green deployment, load balancer setup, and
  autoscaling out of the current MVP setup.
- Revisit the production domain and TLS setup, migration, rollback, and
  zero-downtime strategy before public launch.

### Production Data Durability Gate

- The current EC2 Docker Compose MySQL volume is a development convenience, not
  an approved production durability or backup design.
- Before a production server accepts real user data, explicitly decide and
  document the production database hosting and persistence model, automated
  backup scope and schedule, retention, access control, restore procedure,
  recovery ownership, RPO, and RTO.
- Verify at least one production-like restore rehearsal before declaring the
  production data path ready.
- Back up production data before schema migrations or deployment operations
  with destructive data risk, and document the matching rollback or restore
  path.
- `POLICY_UNDEFINED`: production database service, backup schedule, retention,
  RPO, RTO, and recovery owner remain human decisions until explicitly
  approved.

## Current Dev Infrastructure

- Dev API base URL: `https://3-35-119-70.sslip.io`
- Temporary direct dev API URL: `http://3.35.119.70:8080`
- EC2 instance: `moyeo-api-dev`
- Elastic IP: `3.35.119.70`
- HTTPS reverse proxy: Caddy container `moyeo-caddy`
- Caddy certificate volumes: `moyeo-caddy-data`, `moyeo-caddy-config`
- Dev database: MySQL 8.4 container `moyeo-mysql` on the EC2 Docker Compose
  network
- ECR repository: `moyeo-server`
- Deployment workflow: `.github/workflows/deploy-dev.yml`, executed only from
  `ohujj/MOYEO`
- SSL monitor workflow: `.github/workflows/monitor-ssl.yml`, executed daily and
  available for manual runs only in `ohujj/MOYEO`
- Runtime env file on EC2: `/home/ubuntu/moyeo/.env`
- Departure place search API key: store `KAKAO_LOCAL_REST_API_KEY` only in the
  EC2 runtime `.env`; Docker Compose passes the value into the application
  container.
- Meeting cover storage: use the private Seoul S3 bucket
  `moyeo-meeting-covers-dev-533232489687-ap-northeast-2-an`. Store its name as
  `MEETING_COVER_S3_BUCKET` in the EC2 runtime `.env`; grant the EC2 instance
  role S3 object access instead of storing AWS access keys. The application
  returns a cache-versioned backend image URL, while the object key remains
  private and is never sent to the client. Docker Compose passes
  `MEETING_COVER_S3_BUCKET` and `AWS_REGION` from that runtime `.env` into the
  application container.
- Deployment command path: GitHub Actions -> Amazon ECR -> AWS Systems Manager
  Run Command -> EC2 Docker Compose -> Caddy/Application/MySQL
- Runtime `DB_URL` on the EC2 app container should point to the Compose service
  name:
  `jdbc:mysql://mysql:3306/moyeo?serverTimezone=Asia/Seoul&characterEncoding=UTF-8`.
- Repository mirrors: push verified `main` changes to both `origin` and `cmc`
  while the personal and CMC repositories are maintained together. The CMC
  mirror runs CI only; it does not deploy the dev server.

### Sign in with Apple

- The existing Moyeo App ID is enabled as the primary App ID for Sign in with
  Apple.
- The web Services ID is configured with the dev and production Vercel domains
  and their `/auth/callback/apple` return URLs.
- Apple Key material is stored only in the EC2 runtime `.env` as `APPLE_*`
  values; the downloaded `.p8` source file was removed from the server after
  its Base64 value was stored.
- Required runtime names are `APPLE_OAUTH_ENABLED`, `APPLE_CLIENT_ID`,
  `APPLE_TEAM_ID`, `APPLE_KEY_ID`, `APPLE_PRIVATE_KEY_BASE64`,
  `APPLE_OAUTH_REDIRECT_URI_DEV`, `APPLE_OAUTH_REDIRECT_URI_PROD`, plus
  `APPLE_REFRESH_TOKEN_ENCRYPTION_KEY_BASE64`. `APPLE_REDIRECT_URI` remains a
  temporary compatibility fallback for the dev URI.
  The encryption key must be a separate Base64-encoded random 32-byte value.
  Set `APPLE_OAUTH_ENABLED=true` only when all values are ready; enabled
  configuration is validated at application startup.
- The frontend receives the Apple GET callback and sends the one-time code,
  nonce, and fixed `redirectTarget` (`dev` or `prod`) to the backend
  `POST /api/auth/apple` API. It never sends a URI string. Apple local callback
  testing is unsupported.
- The backend exchanges and verifies the code, identifies the user by Apple's
  `sub`, and issues the Moyeo Access JWT.
- The backend encrypts the Apple refresh token with AES-256-GCM, binds it to the
  verified `sub`, and stores only the ciphertext. Account withdrawal decrypts
  and revokes this stored refresh token without another Apple login.
- Existing Apple rows without ciphertext cannot withdraw until their next
  successful Apple login stores a fresh token; withdrawal then returns
  `503 SOCIAL_LOGIN_UNAVAILABLE` without deleting local data.
- Apple login backend implementation and HTTPS integration verification are
  complete. The remaining work is frontend UI integration with the HTTPS API
  base URL.
- The server maps Apple `redirectTarget=dev` and `redirectTarget=prod` to its
  configured callback URIs. The defaults are
  `https://moyeo-dev.vercel.app/auth/callback/apple` and
  `https://moyeo-web.vercel.app/auth/callback/apple`.
- The dev profile allows Vercel PR Preview origins matching
  `https://moyeo-*-hyeonjirohs-projects.vercel.app` through CORS.
- The production profile allows `https://moyeo-web.vercel.app` through CORS by
  default. Override it with `CORS_ALLOWED_ORIGINS` when the production frontend
  domain changes.
- Docker Compose passes Apple configuration from the EC2 runtime `.env` into
  the application container. Apple secrets must never be committed or logged.

### Kakao Login

- Kakao login uses the REST API authorization-code flow without OpenID Connect.
- Register local, dev, and production Kakao redirect URIs. Their defaults are
  `http://localhost:3000/auth/callback/kakao`,
  `https://moyeo-dev.vercel.app/auth/callback/kakao`, and
  `https://moyeo-web.vercel.app/auth/callback/kakao`.
- The frontend generates a unique `state`, verifies the callback value, and
  sends the one-time authorization code with a fixed `redirectTarget`
  (`local`, `dev`, or `prod`) to `POST /api/auth/kakao`; it never sends a URI
  string.
- The backend exchanges the code with the server-owned REST API key, client
  secret, and exact redirect URI, then uses only the Kakao user-information
  response `id` as `providerUserId`.
- Kakao account withdrawal uses the server-owned Admin Key and stored Kakao
  service user ID to call the Unlink API without another Kakao login.
- Required runtime names are `KAKAO_OAUTH_ENABLED`,
  `KAKAO_OAUTH_REST_API_KEY`, `KAKAO_OAUTH_CLIENT_SECRET`,
  `KAKAO_OAUTH_ADMIN_KEY`, `KAKAO_OAUTH_REDIRECT_URI_LOCAL`,
  `KAKAO_OAUTH_REDIRECT_URI_DEV`, and `KAKAO_OAUTH_REDIRECT_URI_PROD`.
  `KAKAO_OAUTH_REDIRECT_URI` remains a temporary compatibility fallback for the
  dev URI. Set `KAKAO_OAUTH_ENABLED=true` only when all values are ready.
- Keep Kakao OAuth credentials separate from the
  `KAKAO_LOCAL_REST_API_KEY` place-search configuration. Kakao provider tokens
  and all provider secrets must never be committed, stored after login, or
  logged.
- Backend implementation and automated verification are complete. Dev HTTPS
  integration verification requires the runtime OAuth values and a fresh
  one-time authorization code.
- Provider-initiated unlink webhook behavior remains `POLICY_UNDEFINED` and is
  not part of the current login implementation.

## Documentation Policy

- Keep documentation minimal and useful.
- AGENTS.md is the primary working rule for Codex.
- README.md is for humans: setup, run commands, API paths, and basic project
  information.
- docs/00-project-setup.md is for project-level decisions, such as tech choices,
  excluded technologies, and operational roadmap.
- docs/01-dbdiagram.md is for the current database schema in dbdiagram.io DBML
  format.
- docs/policies/ contains canonical API, auth, and domain policy documents.
- docs/ai/ contains AI review rules and reusable review feedback.
- Keep entity comments and DBML notes concise, useful, and aligned when schema
  meaning changes.
- Do not create new markdown documents unless the topic is stable enough to
  maintain.
- For feature-level work, prefer adding a short policy section only when the
  policy is actually needed.
- Do not create separate workflow or review checklist documents unless the
  project complexity justifies them.

## Operational Roadmap

After MVP completion, review these items in sequence as needs become clear:

- Production domain and TLS
- Production database durability, automated backup, and restore rehearsal
- Database migration
- Refresh Token and token rotation
- Error monitoring
- Deployment rollback strategy
- Blue/Green or rolling deployment
- Zero-downtime deployment
- Operational metrics collection
