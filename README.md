<div align="center">

<img src="docs/assets/logo.svg" alt="SAI" width="150">

# Project SAI

**소중한 사람들과 함께할 공간을 만들고, 초대하고, 함께 머무는 웹 애플리케이션**

[![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3-4FC08D?logo=vuedotjs&logoColor=white)](https://vuejs.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

</div>

---

## 한눈에 보기

SAI는 **공간(Space)** 이라는 단위로 사람을 모으는 서비스입니다.
사용자는 자신의 공간을 만들고, 함께하고 싶은 사람에게 초대장을 보냅니다.
초대를 받은 사람이 수락하면 그 공간의 멤버가 되어 함께 머무르게 됩니다.

```text
회원가입 ─▶ 로그인 ─▶ 공간 생성 ─▶ 사용자 초대 ─▶ 수락 / 거절 ─▶ 내 공간 목록
                                    (OWNER)      (PENDING)   (ACCEPTED/DENIED)   (역할별 표시)
```

세션(`JSESSIONID`) 기반 인증을 사용하며, 공간마다 멤버에게 `OWNER` · `MANAGER` · `MEMBER` 역할이 부여됩니다.
초대는 자기 자신에게 보낼 수 없고, 중복 초대와 권한 없는 초대는 서버에서 걸러집니다.

> 학습 목적으로 개발 중인 개인 프로젝트입니다. 인증·초대 흐름의 백엔드 API와 이를 사용하는 Vue 화면이 동작하는 단계입니다.

## 구현 상태

| 영역 | 기능 | 상태 |
| --- | --- | :---: |
| 인증 | 회원가입, BCrypt 비밀번호 해싱 | ✅ |
| 인증 | 로그인 / 로그아웃, 세션 발급 | ✅ |
| 인증 | 로그인 사용자 조회 (`/api/auth/me`) | ✅ |
| 공간 | 공간 생성, 생성자 OWNER 지정 | ✅ |
| 공간 | 참여 중인 공간 목록 (역할 · 인원수 포함) | ✅ |
| 초대 | 초대 보내기, 자기 초대 · 중복 초대 차단 | ✅ |
| 초대 | 초대 수락 / 거절, 보낸 · 받은 초대 목록 | ✅ |
| 공통 | 전역 예외 처리, 일관된 에러 응답 | ✅ |
| 화면 | 로그인 · 회원가입 · 홈(공간 목록, 공간 생성, 초대 보내기) | ✅ |
| 화면 | 받은 초대함 UI (수락 · 거절) | ⏳ |
| 공간 | 공간 삭제, 멤버 관리 | ⏳ |
| 공통 | 공간 내부 콘텐츠 기능 | ⏳ |

## 기술 스택

**Backend** — Java 21 · Spring Boot 4.1 · Spring Web MVC · Spring Data JPA · Spring Validation · Spring Security Crypto(BCrypt) · Flyway · PostgreSQL · Lombok · Gradle 9.5.1(Wrapper)

**Frontend** — Vue 3 · TypeScript · Vite · Vue Router · Pinia · Playwright · oxlint / ESLint / Prettier

## 빠른 시작

### 사전 준비

- JDK 21
- PostgreSQL
- Node.js `^22.18.0 || >=24.12.0`

Gradle은 저장소에 포함된 Wrapper를 사용하므로 따로 설치하지 않습니다.
(Spring Boot 4.1 · Java 21과 호환되지 않는 구버전 Gradle을 직접 지정하면 빌드가 실패합니다.)

### 1. 데이터베이스

```sql
CREATE DATABASE sai;
```

접속 정보는 `backend/src/main/resources/application.properties`에 있으며,
비밀번호는 소스에 두지 않고 `DB_PASSWORD` 환경 변수로 전달합니다.

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/sai
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}
```

첫 실행 시 Flyway가 `backend/src/main/resources/db/migration`의 스크립트로 테이블을 생성합니다.

### 2. 백엔드 실행 → http://localhost:8080

```bash
cd backend
export DB_PASSWORD="본인의 PostgreSQL 비밀번호"
./gradlew bootRun
```

<details>
<summary>Windows PowerShell</summary>

```powershell
cd backend
$env:DB_PASSWORD = "본인의 PostgreSQL 비밀번호"
.\gradlew.bat bootRun
```

</details>

### 3. 프론트엔드 실행 → http://localhost:5173

```bash
cd frontend
npm install
npm run dev
```

백엔드 주소는 `VITE_API_BASE_URL` 환경 변수로 바꿀 수 있으며, 기본값은 `http://localhost:8080`입니다.

## API

모든 응답은 JSON이며, 로그인 이후 요청에는 `JSESSIONID` 쿠키가 필요합니다.

### 인증 `/api/auth`

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/signup` | 회원가입 |
| `POST` | `/login` | 로그인, 세션 생성 후 `userId` 반환 |
| `POST` | `/logout` | 로그아웃, 세션 무효화 |
| `GET` | `/me` | 현재 로그인한 사용자 정보 |

```jsonc
// POST /api/auth/signup
{
  "name": "홍길동",
  "loginId": "hong1234",
  "password": "password123!",
  "phoneNumber": "010-1234-5678",
  "email": "hong@example.com"
}
```

### 공간 `/api/spaces`

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/` | 공간 생성 (요청자가 `OWNER`) |
| `GET` | `/my` | 참여 중인 공간 목록 — 제목, 역할, 멤버 수 |

### 초대 `/api/invitations`

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/` | 초대 보내기 (`spaceId`, `inviteeUserId`) |
| `POST` | `/join` | 초대 수락 → 공간 멤버로 등록 |
| `POST` | `/deny` | 초대 거절 |
| `GET` | `/received` | 내가 받은 초대 목록 |
| `GET` | `/sent` | 내가 보낸 초대 목록 |

## 데이터 모델

```text
users ──< space_member >── space
  │                          │
  └──< space_invitation >────┘
        inviter / invitee, status: PENDING · ACCEPTED · DENIED
```

| 테이블 | 설명 |
| --- | --- |
| `users` | 사용자 계정. `login_id` 유니크, 비밀번호는 해시로만 저장 |
| `space` | 공간 |
| `space_member` | 공간 참여 정보. `(user_id, space_id)` 유니크, `role` 보유 |
| `space_invitation` | 초대장. 초대자 · 피초대자 · 상태 |

자세한 관계는 [`docs/database/ERD.puml`](docs/database/ERD.puml)을 참고하세요.

## 프로젝트 구조

```text
project-sai
├── backend                      Spring Boot 백엔드
│   └── src
│       ├── main/java/com/sai/backend
│       │   ├── auth             회원가입 · 로그인 · 세션
│       │   ├── space            공간 · 멤버 · 초대
│       │   ├── user             사용자 엔티티와 Repository
│       │   ├── common           설정과 세션 상수
│       │   └── global           전역 예외 처리
│       ├── main/resources
│       │   ├── application.properties
│       │   └── db/migration     Flyway 마이그레이션
│       └── test                 Service · Controller · Repository 테스트
├── frontend                     Vue 3 프론트엔드
│   └── src
│       ├── views                로그인 · 회원가입 · 홈
│       ├── components           AppLogo, AuthLayout
│       ├── stores               Pinia (auth, spaces)
│       ├── services             API 클라이언트
│       └── router               라우팅과 인증 가드
└── docs
    ├── architecture             기술 스택 문서
    ├── database                 ERD
    └── devlog                   개발 기록
```

## 테스트

```bash
# 백엔드 — 실제 PostgreSQL 연결이 필요합니다
cd backend
export DB_PASSWORD="본인의 PostgreSQL 비밀번호"
./gradlew test

# 프론트엔드
cd frontend
npm run type-check
npm run test:e2e
```

## 문서

- [기술 스택](docs/architecture/stack.md)
- [ERD](docs/database/ERD.puml)
- [개발 기록](docs/devlog)

## 라이선스

[MIT](LICENSE)
