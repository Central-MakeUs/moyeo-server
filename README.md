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
- Docker, Docker Compose
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

- API Base URL: `http://3.35.119.70:8080`
- Health Check: `http://3.35.119.70:8080/health`
- Swagger UI: `http://3.35.119.70:8080/swagger-ui.html`
- OpenAPI JSON: `http://3.35.119.70:8080/v3/api-docs`

`GET /health` response:

```json
{
  "status": "OK"
}
```

## Current Auth APIs

- `POST /api/auth/apple`
- `GET /api/auth/me`
- `PUT /api/users/me/onboarding`

일반 ID/비밀번호 회원가입·로그인 API는 제공하지 않습니다. Apple 최초 로그인
성공 시 사용자를 즉시 생성하고 Access JWT를 반환하며, 닉네임 등록 전 응답은
`nickname: null`, `onboardingCompleted: false`입니다. 닉네임 등록 전에는 현재
사용자 조회와 온보딩 API 외의 회원 API가 `403 ONBOARDING_REQUIRED`를 반환합니다.

When the `local` or `dev` profile is active, the server creates these idempotent test
accounts and exposes one token endpoint:

- `POST /api/auth/dev/tokens`

The endpoint requires no request body and returns the Access JWT responses for
two fixed direct users. It is not registered in the `prod` profile.

Login responses include an Access JWT.
Protected APIs use the `Authorization: Bearer {accessToken}` header.

Not included yet:

- Refresh Token
- Logout
- Kakao login
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
- Middle-point place recommendations use saved departure coordinates and a temporary commercial-area catalog to return up to five straight-line-distance preview candidates.
- Random place recommendations return up to five candidates from the temporary commercial-area catalog.

Not included yet:

- Step-by-step meeting draft save
- Actual travel-time-based place ranking and final place confirmation
- Current-location lookup
- Tmap/Tmap Transit integration
- Persistent commercial-area data import and management
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
→ EC2 Docker Compose deployment
→ EC2 Docker Compose MySQL connection
```

Runtime components:

- EC2: `moyeo-api-dev`
- MySQL container: `moyeo-mysql`
- ECR repository: `moyeo-server`
- App container: `moyeo-server`

Security policy for dev:

- API port `8080` is public for frontend development and testing.
- SSH port `22` is restricted to the developer IP.
- MySQL port `3306` is not publicly exposed.
- MySQL may be bound to EC2 localhost `127.0.0.1:3306` for DBeaver access through SSH tunneling.
- GitHub Actions deploys through AWS Systems Manager instead of opening SSH to GitHub Actions runners.

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
APPLE_REDIRECT_URI
KAKAO_LOCAL_REST_API_KEY
MEETING_COVER_S3_BUCKET
```

Apple 로그인 활성화 시 모든 `APPLE_*` 값을 설정하고
`APPLE_OAUTH_ENABLED=true`로 지정합니다. `.p8` 개인키는 파일 전체를 Base64로
인코딩한 값만 `APPLE_PRIVATE_KEY_BASE64`에 저장하며 원문과 실제 값은 커밋하거나
로그에 출력하지 않습니다.

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
