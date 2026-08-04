# Moyeo Server

CMC 모여(Moyeo) 프로젝트의 Spring Boot 기반 MVP 백엔드 서버입니다.

현재 서버는 기본 실행 환경, health check, Swagger/OpenAPI, 공통 오류 응답, 소셜 로그인 기반 구조, dev 배포 환경을 포함합니다.

## Tech Stack

- Java 21
- Spring Boot 3.5.15
- Gradle
- Spring Web, Validation, Data JPA
- H2(local/test)
- MySQL(dev/prod)
- Springdoc OpenAPI
- Spring Boot Actuator
- JUnit 5
- Docker, Docker Compose, Caddy
- AWS EC2, ECR, EC2 Docker Compose MySQL, Systems Manager
- GitHub Actions

## Local Run

macOS/Linux:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

Windows PowerShell:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

Default local port:

```text
8080
```

## Test and Build

macOS/Linux:

```bash
./gradlew test
./gradlew build
```

Windows PowerShell:

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

## API Paths

Local:

- Health Check: `GET http://localhost:8080/health`
- Actuator Health: `GET http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Dev Server:

- API Base URL: `https://3-35-119-70.sslip.io`
- Health Check: `https://3-35-119-70.sslip.io/health`
- Swagger UI: `https://3-35-119-70.sslip.io/swagger-ui.html`
- OpenAPI JSON: `https://3-35-119-70.sslip.io/v3/api-docs`

The former direct endpoint `http://3.35.119.70:8080` remains temporarily
available while the dev frontend moves to HTTPS.

Apple login has been verified end to end from the dev Vercel origin through the
HTTPS API, including browser CORS preflight and Access JWT issuance. Frontend UI
integration remains.

TODO: After the frontend application is deployed with the HTTPS API base URL,
remove public port `8080` from the EC2 security group and stop publishing the
app container port to the public host.

`GET /health` response:

```json
{
  "status": "OK"
}
```

## Current Auth APIs

- `POST /api/auth/apple`
- `POST /api/auth/kakao`
- `GET /api/auth/me`
- `PUT /api/users/me/onboarding`
- `DELETE /api/users/me`

Social account withdrawal requires only the current Moyeo Access JWT and no
request body. The backend revokes the encrypted stored Apple refresh token or
uses the server-owned Kakao Admin Key to unlink the stored Kakao user ID before
committing local account deletion.

일반 ID/비밀번호 회원가입·로그인 API는 제공하지 않습니다. Apple 또는 카카오 최초
로그인 성공 시 사용자를 즉시 생성하고 Access JWT를 반환하며, 닉네임 등록 전 응답은
`nickname: null`, `onboardingCompleted: false`입니다. 닉네임 등록 전에는 현재
사용자 조회와 온보딩 API 외의 회원 API가 `403 ONBOARDING_REQUIRED`를 반환합니다.

When the `local` or `dev` profile is active, the server creates these idempotent test
accounts and exposes one token endpoint:

- `POST /api/auth/dev/tokens`

The endpoint requires no request body and returns the Access JWT responses for
two fixed direct users. The returned development tokens are deterministic and
expire on 2099-01-01, so they remain usable after a dev-server restart while
the JWT secret is unchanged. It is not registered in the `prod` profile.

Login responses include an Access JWT.
Protected APIs use the `Authorization: Bearer {accessToken}` header.

Not included yet:

- Refresh Token
- Logout
- Social account linking

## Current Meeting APIs

The current meeting implementation covers the first milestone base flow.

- `POST /api/meetings`
- `POST /api/departure-places/searches`
- `GET /api/me/places`
- `POST /api/me/places`
- `PATCH /api/me/places/{savedPlaceId}`
- `DELETE /api/me/places/{savedPlaceId}`
- `GET /api/meetings/invitations/{inviteCode}`
- `GET /api/meetings/invitations/{inviteCode}/view`
- `GET /api/meetings/invitations/{inviteCode}/view/schedules`
- `GET /api/meetings/invitations/{inviteCode}/view/places`
- `POST /api/meetings/invitations/{inviteCode}/guests`
- `POST /api/meetings/invitations/{inviteCode}/members`
- `PUT /api/meetings/invitations/{inviteCode}/participants/{participantId}/participation`

Current meeting scope:

- A logged-in user can create a meeting as host.
- Meeting creation for the first MVP accepts the first creation flow settings in one request.
- The server issues an invite code.
- INV-01 invite entry uses public invite-code lookup and returns meeting basic information plus participation availability status.
- A guest can join with nickname and password.
- Guest join does not accept departure address, coordinates, or transportation mode directly.
- Participant nicknames are unique only inside each meeting.
- `deadlineAt` is calculated by the server from request `deadlineMinutes`.
- `deadlineMinutes` is accepted in 10-minute units from 10 minutes up to 72 hours.
- Schedule voting applies the same available time range to every selected candidate date.
- Schedule voting time ranges are accepted in 1-hour units.
- Guest participation is rejected after `deadlineAt`.
- Invite-code lookup returns whether the current meeting can still be joined and the reason/message when joining is blocked.
- Middle-point creation stores the host departure name, address, coordinates, and transportation mode as the host participant snapshot.
- `POST /api/departure-places/searches` searches subway stations, road-name or lot-number addresses, and general places through Kakao Local. Exact `~역` queries use subway-station results first; successful zero-result primary searches fall back to a general place keyword search.
- Web guests use the same departure search path with an `inviteCode` query parameter and no Access JWT. The server validates the invite code before calling Kakao Local; a present invalid token never falls back to invite-code access.
- Departure-place search candidates include WGS84 latitude and longitude. A client using this search sends the selected coordinate pair with the existing meeting creation or participation request; it must not geocode the address again when saving. The existing request rule still requires latitude and longitude to be sent together, and a legacy request that omits both remains a coordinate-less snapshot handled as `COORDINATES_PENDING`.
- Place recommendation strategy is fixed after meeting creation in the first MVP.
- INV-02 participation input stores schedule availability for schedule-coordination meetings.
- INV-02 participation input stores departure address, coordinates, and transportation mode for place-coordination meetings.
- A participation save request replaces the participant's previous schedule availability slots.
- Public pre-confirmation meeting views provide participant lists, schedule candidates, and place recommendations.
- Schedule candidates are calculated from saved availability slots and can be sorted by longest meeting time or earliest date; each request returns up to three candidates.
- Middle-point place recommendations use saved departure coordinates and the persistent Seoul commercial-area catalog to return up to five straight-line-distance preview candidates.
- Random place recommendations return up to five candidates from the persistent Seoul commercial-area catalog.

Not included yet:

- Step-by-step meeting draft save
- Actual travel-time-based place ranking and final place confirmation
- Current-location lookup
- Tmap/Tmap Transit integration
- Voting/free-poll
- Final decision/result
- Meeting list/detail tabs
- Meeting edit/delete
- Guest re-entry authentication

## Dev Deployment

The dev server is deployed on AWS.

The dev profile currently uses Hibernate schema update while the MVP schema is still changing. Treat this as temporary development convenience, not a production migration strategy.

```text
GitHub Actions
→ Gradle test/build
→ Docker image build
→ Amazon ECR push
→ AWS Systems Manager Run Command
→ EC2 Docker Compose deployment (Caddy + application + MySQL)
→ EC2 Docker Compose MySQL connection
```

Runtime components:

- EC2: `moyeo-api-dev`
- MySQL container: `moyeo-mysql`
- ECR repository: `moyeo-server`
- App container: `moyeo-server`
- HTTPS reverse-proxy container: `moyeo-caddy`

Security policy for dev:

- HTTPS ports `80` and `443` are public for Caddy certificate issuance,
  HTTP-to-HTTPS redirection, and API traffic.
- API port `8080` remains temporarily public for frontend migration and direct
  troubleshooting. Remove this exception after the dev frontend application is
  deployed with the verified HTTPS endpoint.
- SSH port `22` is restricted to the developer IP.
- MySQL port `3306` is not publicly exposed.
- MySQL may be bound to EC2 localhost `127.0.0.1:3306` for DBeaver access through SSH tunneling.
- GitHub Actions deploys through AWS Systems Manager instead of opening SSH to GitHub Actions runners.
- Only `ohujj/MOYEO` runs the `Deploy Dev` job. The CMC mirror runs CI but
  skips dev deployment so mirrored pushes cannot deploy the same EC2 instance
  twice.

## Environment Variables

`dev` and `prod` profiles require environment variables.

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
CORS_ALLOWED_ORIGINS
APPLE_OAUTH_ENABLED
APPLE_CLIENT_ID
APPLE_TEAM_ID
APPLE_KEY_ID
APPLE_PRIVATE_KEY_BASE64
APPLE_OAUTH_REDIRECT_URI_DEV
APPLE_OAUTH_REDIRECT_URI_PROD
APPLE_REFRESH_TOKEN_ENCRYPTION_KEY_BASE64
KAKAO_OAUTH_ENABLED
KAKAO_OAUTH_REST_API_KEY
KAKAO_OAUTH_CLIENT_SECRET
KAKAO_OAUTH_ADMIN_KEY
KAKAO_OAUTH_REDIRECT_URI_LOCAL
KAKAO_OAUTH_REDIRECT_URI_DEV
KAKAO_OAUTH_REDIRECT_URI_PROD
KAKAO_LOCAL_REST_API_KEY
MEETING_COVER_S3_BUCKET
```

`DEV_API_DOMAIN` is optional and defaults to `3-35-119-70.sslip.io`. Caddy uses
it as the dev HTTPS host and stores certificate state in the persistent
`moyeo-caddy-data` Docker volume.

Caddy manages certificate issuance and renewal automatically. Keep public ports
`80` and `443` reachable and do not delete the `moyeo-caddy-data` or
`moyeo-caddy-config` volumes during ordinary deployments.

The `Monitor SSL Certificate` GitHub Actions workflow checks the public
certificate daily and fails if TLS or hostname verification fails, or if fewer
than 21 days remain before expiration. It runs outside EC2 and therefore adds no
resident process or memory usage to the server. The check runs only in
`ohujj/MOYEO`, not in the CMC mirror.

Apple 로그인 활성화 시 모든 `APPLE_*` 값을 설정하고
`APPLE_OAUTH_ENABLED=true`로 지정합니다. `.p8` 개인키는 파일 전체를 Base64로
인코딩한 값만 `APPLE_PRIVATE_KEY_BASE64`에 저장하며 원문과 실제 값은 커밋하거나
로그에 출력하지 않습니다. `APPLE_REFRESH_TOKEN_ENCRYPTION_KEY_BASE64`는 별도의
무작위 32바이트 키를 Base64로 인코딩한 값이며, DB와 분리해 런타임 환경에만
보관합니다.

카카오 로그인 활성화 시 `KAKAO_OAUTH_REST_API_KEY`,
`KAKAO_OAUTH_CLIENT_SECRET`, `KAKAO_OAUTH_ADMIN_KEY`, 정확한
`KAKAO_OAUTH_REDIRECT_URI`를 설정하고
`KAKAO_OAUTH_ENABLED=true`로 지정합니다. 프론트엔드는 콜백의 `state`를 검증한
뒤 일회용 인가 코드만 `POST /api/auth/kakao`로 전달합니다. 로그인 설정은 장소
검색용 `KAKAO_LOCAL_REST_API_KEY`와 이름을 분리하며, 실제 키와 시크릿은 런타임
환경에만 저장합니다.

기존 운영 DB에 소셜 로그인을 처음 배포하기 전에는 DB를 백업하고
`scripts/db/2026-07-24-social-login.sql`을 1회 적용해야 합니다. 기존 DB에서
재로그인 없는 Apple 탈퇴를 활성화하기 전에는
`scripts/db/2026-07-26-social-refresh-token.sql`도 1회 적용해야 합니다. 운영 프로필의
기본 CORS 프론트 주소는 `https://moyeo-web.vercel.app`이며, 변경 시
`CORS_ALLOWED_ORIGINS`로 덮어씁니다.

`KAKAO_LOCAL_REST_API_KEY` is the Kakao Local REST API key used only by the
server for departure place search.
Keep the real key only in the runtime environment; do not add it to source code,
configuration files, or GitHub Actions deployment commands.

`MEETING_COVER_S3_BUCKET` is the private S3 bucket used to retain resized meeting
cover images. The EC2 instance role, not an AWS access key in the environment,
must have access to this bucket.

Dev CORS origin example:

```text
CORS_ALLOWED_ORIGINS=https://moyeo-web.vercel.app,https://moyeo-dev.vercel.app,http://localhost:3000
```

The dev profile separately allows Vercel PR Preview origins matching
`https://moyeo-*-hyeonjirohs-projects.vercel.app`.

The EC2 dev server stores runtime values in:

```text
/home/ubuntu/moyeo/.env
```

Add the departure place-search key to that file before deploying or recreating the app
container:

```text
KAKAO_LOCAL_REST_API_KEY=your-kakao-local-rest-api-key
MEETING_COVER_S3_BUCKET=moyeo-meeting-covers-dev-533232489687-ap-northeast-2-an
```

Do not commit real secrets to the repository.

## Logging

The `dev` and `prod` profiles write application logs to `/app/logs/moyeo.log`
and `ERROR`-level exception logs to `/app/logs/moyeo-error.log`. Logs roll over
daily and again at 25 MB per file. They are kept for up to 30 days with a
combined size cap of 256 MB (general 192 MB, exception 64 MB). The local
profile keeps console-only logging. Each HTTP response contains an
`X-Trace-Id` header, and the same ID is included in request logs.

For Docker Compose, logs are persisted to `./logs` by default. Set `LOG_DIR` in
the runtime environment to use a different host directory.

## Documentation

- Codex working rules: `AGENTS.md`
- Project setup and technical decisions: `docs/00-project-setup.md`
- DB diagram DBML: `docs/01-dbdiagram.md`
- Product-design flow map and backend routing: `docs/02-ui-flow-map.md`
