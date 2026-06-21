# Moyeo Server

CMC 모여 프로젝트의 6주 MVP 개발을 위한 Spring Boot 서버입니다. 현재는 도메인 기능 없이 실행, 연결 확인, API 문서, CI를 위한 최소 기반만 제공합니다.

## 기술 스택

- Java 21
- Spring Boot 3.5.15
- Gradle
- Spring Web, Validation, Data JPA
- H2(local/test), MySQL(dev/prod configuration template)
- Springdoc OpenAPI, Spring Boot Actuator
- JUnit 5

## 로컬 실행

macOS/Linux:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

Windows PowerShell:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

기본 포트는 `8080`입니다.

## API 확인

- 서버 health check: `GET http://localhost:8080/health`
- Actuator health: `GET http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

`GET /health` 응답:

```json
{
  "status": "OK"
}
```

## 테스트와 빌드

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

## dev/prod 데이터베이스 설정 틀

`dev` 또는 `prod` profile을 사용할 때 다음 환경변수를 외부에서 주입합니다. 실제 값과 secret은 저장소에 커밋하지 않습니다.

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
