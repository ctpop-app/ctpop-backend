# CT Pop 인증 서비스

휴대폰 번호 기반 OTP 인증 및 JWT 토큰 관리를 제공하는 RESTful API 서비스입니다.

## 기술 스택

- Java 17
- Spring Boot 3.4.5
- Spring Security
- Redis
- Twilio API (SMS)
- JWT (인증)

## 서비스 구성 및 포트

프로젝트는 다음과 같이 구성되어 있습니다:

| 서비스 | 포트 | 설명 |
|-------|------|------|
| 백엔드 API | 8080 | Spring Boot 애플리케이션 |
| Redis | 6379 | 데이터 캐싱 및 토큰 저장소 (로컬 설치) |

### 접근 URL

- **백엔드 API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Redis**: localhost:6379 (Redis 클라이언트로 접근)

## 시작하기

### 사전 요구 사항

- JDK 17 이상
- Redis 서버 (로컬 설치)
- Twilio 계정 (OTP SMS 발송용)

### 환경 설정

1. `.env` 파일이 있는지 확인하고 다음 값들이 설정되어 있는지 확인합니다:

```
# Twilio API 인증 정보
TWILIO_SID=your_actual_twilio_account_sid
TWILIO_TOKEN=your_actual_twilio_auth_token
TWILIO_VERIFY_SID=your_actual_twilio_verify_service_sid

# JWT 설정
JWT_SECRET=your_actual_jwt_secret_key_at_least_32_characters_long

# 필요시 추가
SPRING_PROFILES_ACTIVE=prod
```

### 로컬에서 실행하기

1. Redis 서버가 실행 중인지 확인합니다.

2. 애플리케이션 빌드 및 실행:

```bash
./gradlew bootRun
```

3. 또는 JAR 파일 생성 후 실행:

```bash
./gradlew bootJar
java -jar build/libs/ctpop-backend-0.0.1-SNAPSHOT.jar
```

## API 엔드포인트

서비스가 실행되면 다음 엔드포인트를 사용할 수 있습니다:

- `POST /api/auth/otp/send`: OTP 코드 발송
- `POST /api/auth/otp/verify`: OTP 코드 검증 및 토큰 발급
- `POST /api/auth/refresh`: 액세스 토큰 갱신
- `POST /api/auth/logout`: 로그아웃 (토큰 무효화)
- `GET /api/auth/echo`: 에코 테스트 (서버 연결 확인용)

API 문서는 아래 URL에서 확인할 수 있습니다:
- Swagger UI: http://localhost:8080/swagger-ui.html 