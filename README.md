# Moyeo Server

> 함께 만날 시간과 장소를 정하는 Moyeo의 Spring Boot 백엔드입니다.

모임 생성부터 초대 참여, 일정·장소 조율과 확정까지의 MVP 흐름을 제공합니다.
상세 API 계약은 Swagger/OpenAPI를 기준으로 관리합니다.

**현재 기준:** 2026-08-06 · Java 21 · Spring Boot 3.5.15

## 한눈에 보기

| 영역 | 제공 기능 |
| --- | --- |
| 인증 | Apple·Kakao 로그인, Access JWT, 닉네임 온보딩, 회원 탈퇴 |
| 모임 | 생성, 초대 링크, 회원·웹 게스트 참여, 참여 응답 수정, 나가기 |
| 조율 | 일정 가능 시간 집계, 중간 지점·실제 이동시간 장소 추천, 방장 확정 |
| 개인화 | 마이페이지, 프로필 색상, 저장 출발지, 피드백 |
| 운영 | Health/Actuator, Swagger, 요청 추적, Docker Compose 기반 dev 배포 |

## 빠른 시작

### 요구 사항

- JDK 21
- 별도 데이터베이스 없이 실행 가능: `local` 프로필은 H2 메모리 DB를 사용

### 실행

```bash
# macOS / Linux
./gradlew bootRun --args='--spring.profiles.active=local'

# Windows PowerShell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

서버는 기본적으로 `http://localhost:8080`에서 실행됩니다.

| 확인 항목 | 주소 |
| --- | --- |
| Health | `GET /health` |
| Actuator Health | `GET /actuator/health` |
| Swagger UI | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) |
| OpenAPI JSON | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) |

### 검증

```bash
# macOS / Linux
./gradlew test
./gradlew build

# Windows PowerShell
.\gradlew.bat test
.\gradlew.bat build
```

## 주요 흐름

```text
소셜 로그인 → 닉네임 온보딩 → 모임 생성 또는 초대 링크 진입
                                  ↓
                   일정·출발지 응답 수집 → 추천 확인 → 방장 확정
```

- 모임은 `SCHEDULE_ONLY`, `PLACE_ONLY`, `SCHEDULE_AND_PLACE`로 생성합니다.
- 초대 링크는 공개 조회할 수 있으며, 웹에서는 닉네임·4자리 비밀번호로 게스트 참여를 지원합니다.
- 출발지 검색과 저장은 서울·경기 지역을 대상으로 합니다. 좌표는 검색 결과를 그대로 사용합니다.
- 장소 조율은 인원이 모두 찬 뒤 Kakao 경로 기반 실제 이동시간 추천을 저장해 재사용합니다.
- 일정 또는 장소 확정은 방장만 가능하며, 확정에는 활성 참여자 2명 이상이 필요합니다.

현재 지원하지 않는 범위는 단계별 임시저장, 현재 위치 조회, 자유 투표, 직접 초대 외의 회원·그룹 초대입니다.

## API 사용

Swagger UI가 요청·응답 DTO, 인증 요구사항, enum, 오류 응답의 단일 기준입니다.

- 로컬: [Swagger UI](http://localhost:8080/swagger-ui.html)
- 개발 서버: [Swagger UI](https://3-35-119-70.sslip.io/swagger-ui.html)
- 개발 서버 Health: [https://3-35-119-70.sslip.io/health](https://3-35-119-70.sslip.io/health)

보호된 API는 다음 헤더를 사용합니다.

```http
Authorization: Bearer {accessToken}
```

성공 응답은 DTO를 직접 반환합니다. 오류는 RFC 9457 Problem Details
(`application/problem+json`) 형식이며, 클라이언트 분기용 `code`를 포함합니다.
모든 응답에는 로그 추적용 `X-Trace-Id` 헤더가 포함됩니다.

`local`·`dev` 프로필에서는 `POST /api/auth/dev/tokens`로 두 테스트 계정의
장기 Access Token을 발급할 수 있습니다. 이 경로는 `prod` 프로필에 노출되지 않습니다.

## 기술 구성

- Java 21, Spring Boot 3.5.15, Gradle
- Spring Web, Validation, Data JPA, Spring Security Crypto
- H2 (`local`/test), MySQL (`dev`/`prod`)
- Springdoc OpenAPI, Spring Boot Actuator, JUnit 5
- Docker Compose, Caddy, AWS EC2/ECR/Systems Manager, GitHub Actions

## 환경 설정

`dev`·`prod`는 런타임 환경 변수로 설정합니다. 실제 키와 시크릿은 소스, 로그,
GitHub Actions 명령에 넣지 않습니다.

| 범주 | 주요 환경 변수 |
| --- | --- |
| 데이터베이스 | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` |
| 인증/CORS | `JWT_SECRET`, `CORS_ALLOWED_ORIGINS` |
| Apple 로그인 | `APPLE_OAUTH_ENABLED`, `APPLE_CLIENT_ID`, `APPLE_TEAM_ID`, `APPLE_KEY_ID`, `APPLE_PRIVATE_KEY_BASE64` |
| Kakao 로그인·장소 | `KAKAO_OAUTH_*`, `KAKAO_LOCAL_REST_API_KEY`, `KAKAO_ROUTE_REST_API_KEY` |
| 커버 이미지 | `MEETING_COVER_S3_BUCKET`, `AWS_REGION` |

`KAKAO_ROUTE_REST_API_KEY`가 없으면 권한이 있는 `KAKAO_LOCAL_REST_API_KEY`를
경로 조회에도 사용합니다. 실제 이동시간 추천 수는
`MEETING_ACTUAL_ROUTE_PRELIMINARY_CANDIDATE_COUNT`,
`MEETING_ACTUAL_ROUTE_FINAL_RECOMMENDATION_COUNT`로 조정할 수 있습니다.

전체 환경 변수, CORS, 로그, 배포 절차는 [프로젝트 설정 문서](docs/00-project-setup.md)를
확인합니다.

## 개발 배포

개발 환경은 GitHub Actions → Gradle 검증/이미지 빌드 → ECR → AWS Systems Manager →
EC2 Docker Compose(Caddy, 애플리케이션, MySQL) 흐름으로 배포합니다.

- HTTPS 개발 도메인: `3-35-119-70.sslip.io`
- Caddy가 TLS 인증서 발급·갱신을 관리합니다.
- MySQL은 외부에 공개하지 않고, 필요 시 SSH 터널로 접근합니다.

운영 전제와 내구성·백업 규칙은 반드시
[프로젝트 설정 문서](docs/00-project-setup.md)를 따릅니다.

## 문서 안내

| 문서 | 용도 |
| --- | --- |
| [프로젝트 설정](docs/00-project-setup.md) | MVP 범위, 기술 결정, 배포, 운영 준비 |
| [API 정책](docs/policies/API_POLICY.md) | 성공·오류 응답, Swagger 기준 |
| [인증 정책](docs/policies/AUTH_POLICY.md) | 로그인, JWT, 보안 규칙 |
| [모임 참여 정책](docs/policies/MEETING_PARTICIPATION_POLICY.md) | 생성, 초대, 참여, 조율, 확정 |
| [DB 다이어그램](docs/01-dbdiagram.md) | DBML 및 스키마 계약 |
| [UI 흐름 맵](docs/02-ui-flow-map.md) | 화면 흐름과 백엔드 라우팅 |
| [작업 규칙](AGENTS.md) | 변경·검증·문서 동기화 규칙 |

## README 유지 관리

README는 시작 방법과 현재 제공 기능의 요약만 유지합니다. API 세부 스펙은 Swagger,
도메인 정책은 정책 문서를 기준으로 합니다. Markdown 문서를 변경할 때는 README에
영향이 있는지 반드시 검토하고, 영향이 있으면 같은 변경에 반영합니다.
