# Project SAI

Project SAI는 사용자 인증을 기반으로 공간을 만들고, 사용자가 공간에 참여하거나 다른 사용자를 초대할 수 있도록 개발 중인 웹 애플리케이션입니다.

현재 백엔드의 회원가입과 로그인, 세션 저장 기능까지 구현되어 있으며 프론트엔드는 Vue 기본 프로젝트가 구성된 상태입니다.

## 주요 기능

현재 구현된 기능은 다음과 같습니다.

- 회원가입
- BCrypt 기반 비밀번호 암호화
- 로그인
- 로그인 성공 시 세션 생성 및 사용자 ID 저장
- 회원가입 및 로그인 예외 처리
- PostgreSQL 및 Flyway 연동

앞으로 구현할 주요 기능은 다음과 같습니다.

- 현재 로그인한 사용자 조회
- 로그아웃 및 인증되지 않은 요청 처리
- 회원 정보 관리
- 공간 생성 및 참여
- 사용자 초대 및 초대 상태 관리
- Vue 프론트엔드와 백엔드 API 연동

## 기술 스택

### Backend

- Java 21
- Spring Boot 4.1
- Gradle 9.5.1
- Spring Web MVC
- Spring Data JPA
- Spring Validation
- Spring Security Crypto
- Flyway
- PostgreSQL
- Lombok

### Frontend

- Vue 3
- TypeScript
- Vite
- Vue Router
- Pinia
- Playwright

## 프로젝트 구조

```text
project-sai
├── backend                 Spring Boot 백엔드
│   ├── src/main/java
│   │   └── com/sai/backend
│   │       ├── auth        회원가입 및 로그인
│   │       ├── common      공통 설정과 세션 상수
│   │       ├── global      공통 예외 처리
│   │       └── user        사용자 엔티티와 Repository
│   └── src/main/resources
│       ├── application.properties
│       └── db/migration    Flyway 마이그레이션
├── frontend                Vue 프론트엔드
└── docs
    ├── architecture        기술 스택 문서
    ├── database            ERD
    └── devlog              개발 기록
```

## 사전 준비

아래 프로그램이 필요합니다.

- JDK 21
- PostgreSQL
- Node.js 22.18 이상 또는 24.12 이상
- npm
- Git

Gradle은 프로젝트에 포함된 Gradle Wrapper를 사용하므로 별도로 설치할 필요가 없습니다.

현재 확인된 JDK 경로는 다음과 같습니다.

```text
C:\Program Files\Java\jdk-21.0.12
```

## 저장소 받기

```powershell
git clone https://github.com/HedgehogCoyote/project-sai.git
cd project-sai
```

## PostgreSQL 설정

PostgreSQL을 실행하고 `sai` 데이터베이스를 생성합니다.

```sql
CREATE DATABASE sai;
```

백엔드는 다음 설정으로 데이터베이스에 연결합니다.

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/sai
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}
```

데이터베이스 비밀번호는 소스 코드에 저장하지 않고 `DB_PASSWORD` 환경 변수로 전달합니다.

PowerShell에서 현재 터미널에만 설정하려면 다음 명령을 사용합니다.

```powershell
$env:DB_PASSWORD = "본인의 PostgreSQL 비밀번호"
```

STS에서 실행할 때는 다음 위치에 환경 변수를 추가합니다.

```text
Run Configurations
→ Spring Boot App
→ Environment
→ DB_PASSWORD 추가
```

애플리케이션을 처음 실행하면 Flyway가 `backend/src/main/resources/db/migration`의 SQL을 이용해 테이블을 생성합니다.

## STS에서 백엔드 열기

STS 워크스페이스와 프로젝트 폴더는 서로 다르게 지정해야 합니다.

권장 워크스페이스 예시는 다음과 같습니다.

```text
C:\Users\SSAFY\sts-workspace
```

다음 순서로 백엔드를 가져옵니다.

1. `File → Import`를 선택합니다.
2. `Gradle → Existing Gradle Project`를 선택합니다.
3. 프로젝트 경로로 `C:\Project\project-sai\backend`를 지정합니다.
4. Gradle Distribution은 `Gradle Wrapper`를 선택합니다.
5. Java Home은 `C:\Program Files\Java\jdk-21.0.12`를 지정합니다.
6. 가져오기가 끝나면 `Gradle → Refresh Gradle Project`를 실행합니다.

Gradle 8.9를 직접 선택하면 현재 Spring Boot 버전 및 Java 버전과 호환되지 않으므로 프로젝트에 포함된 Wrapper를 사용해야 합니다.

## 백엔드 실행

PowerShell에서 다음 명령을 실행합니다.

```powershell
cd backend
$env:DB_PASSWORD = "본인의 PostgreSQL 비밀번호"
.\gradlew.bat bootRun
```

정상적으로 실행되면 기본 주소는 다음과 같습니다.

```text
http://localhost:8080
```

테스트만 실행하려면 다음 명령을 사용합니다.

```powershell
cd backend
$env:DB_PASSWORD = "본인의 PostgreSQL 비밀번호"
.\gradlew.bat test
```

현재 테스트는 실제 PostgreSQL 연결을 사용하므로 테스트 실행 전에도 데이터베이스와 환경 변수가 준비되어 있어야 합니다.

## 프론트엔드 실행

새 PowerShell을 열어 다음 명령을 실행합니다.

```powershell
cd frontend
npm install
npm run dev
```

Vite의 기본 개발 서버 주소는 다음과 같습니다.

```text
http://localhost:5173
```

타입 검사와 프로덕션 빌드는 다음 명령으로 실행합니다.

```powershell
npm run type-check
npm run build
```

## 인증 API

### 회원가입

```http
POST /api/auth/signup
Content-Type: application/json
```

요청 예시:

```json
{
  "name": "홍길동",
  "loginId": "hong1234",
  "password": "password123!",
  "phoneNumber": "010-1234-5678",
  "email": "hong@example.com"
}
```

### 로그인

```http
POST /api/auth/login
Content-Type: application/json
```

요청 예시:

```json
{
  "loginId": "hong1234",
  "password": "password123!"
}
```

로그인에 성공하면 응답에 사용자 ID가 포함되며 `JSESSIONID` 쿠키가 생성됩니다. 이후 세션 인증 요청에서는 이 쿠키를 함께 전달해야 합니다.

## 현재 개발 상태

- [x] Spring Boot 및 Vue 프로젝트 구성
- [x] PostgreSQL 연결
- [x] Flyway 초기 스키마 작성
- [x] User 엔티티와 Repository 구현
- [x] 회원가입 API 구현
- [x] 로그인 API 구현
- [x] 세션에 로그인 사용자 ID 저장
- [x] 공통 인증 예외 응답 구현
- [ ] 로그인 사용자 조회 API
- [ ] 로그아웃 API
- [ ] 인증되지 않은 요청 처리
- [ ] 인증 Service 및 Controller 테스트
- [ ] Space 도메인 구현
- [ ] Vue 화면 및 API 연동

## 다음 작업 권장 순서

1. STS에서 Gradle Wrapper와 JDK 21을 사용해 백엔드를 정상 실행합니다.
2. Postman으로 회원가입과 로그인을 다시 확인합니다.
3. `GET /api/auth/me` 로그인 사용자 조회 API를 구현합니다.
4. `POST /api/auth/logout` 로그아웃 API를 구현합니다.
5. 인증 API 테스트를 작성합니다.
6. Vue 로그인 및 회원가입 화면을 구현합니다.
7. Space 도메인 개발을 시작합니다.

## 관련 문서

- 기술 스택: `docs/architecture/stack.md`
- ERD: `docs/database/ERD.puml`
- 개발 기록: `docs/devlog`
