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

## 인증 시스템

### 인증 흐름

1. **OTP 인증 (최초/로그아웃/회원탈퇴/30일 이상 미접속)**
   - OTP 인증 성공 시 액세스 토큰(짧은 유효기간)과 리프레시 토큰(30일) 발급
   - 액세스 토큰은 인증 직후 보호된 API(프로필 등록 등)에만 사용
   - UUID는 OTP 인증 시 생성되어 사용자의 고유 식별자로 사용됨

2. **일반 인증 (앱 재접속/로그인)**
   - 리프레시 토큰으로만 인증
   - 리프레시 토큰 유효기간(30일) 검증
   - 검증 통과 시 새로운 리프레시 토큰 재발급(기존 토큰 무효화)
   - 액세스 토큰은 필요 시만 발급

### API 엔드포인트

#### 테스트 관련 (`/test/*`)
- `GET /echo` - 서버 연결 테스트
  - Request: `?message=test`
  - Response: `"test"`

#### OTP 관련 (`/auth/otp/*`)
- `POST /send` - OTP 전송
  - Request: `{ "phone": "01012345678" }`
  - Response: `204 No Content`

- `POST /verify` - OTP 검증 및 토큰 발급
  - Request: `{ "phone": "01012345678", "code": "123456" }`
  - Response: `{ "accessToken": "...", "refreshToken": "...", "uuid": "..." }`

#### 토큰 관련 (`/auth/*`)
- `POST /refresh` - 리프레시 토큰 검증 및 재발급
  - Request: `{ "uuid": "...", "refreshToken": "..." }`
  - Response: `{ "refreshToken": "...", "uuid": "..." }`

- `POST /logout` - 로그아웃
  - Request: `{ "uuid": "..." }`
  - Response: `204 No Content`

### 토큰 관리

- **UUID 기반 토큰 관리**
  - 모든 토큰은 UUID를 기준으로 생성 및 관리됨
  - Redis에 토큰 저장 시 UUID를 키로 사용
  - 토큰의 subject와 claim에 UUID 포함

- **토큰 유효기간**
  - 액세스 토큰: 30분
  - 리프레시 토큰: 30일

### 예외 처리
- `OtpException`: OTP 관련 예외 (전송 실패, 검증 실패 등)
- `TokenException`: 토큰 관련 예외 (검증 실패, 만료, 변조 등) 