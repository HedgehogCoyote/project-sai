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
초대는 공간 멤버만 보낼 수 있습니다. 자기 자신과 이미 참여 중인 사용자에게 보낼 수 없으며, 같은 초대자가 같은 공간의 같은 사용자에게 보낸 대기 중 초대는 서비스에서 중복 검사합니다. 역할별 초대 권한과 동시 요청의 중복 방지는 아직 보완할 부분입니다.

> 학습 목적으로 개발 중인 개인 프로젝트입니다. 인증·공간·초대는 백엔드 API를 사용하며, 공간 안의 세부 기능은 브라우저 임시 데이터로 먼저 구현했습니다.

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
| 화면 | 로그인 · 회원가입, 가입 후 자동 로그인 | ✅ |
| 화면 | 인증 가드, 세션 만료 시 로그인 이동, 로그아웃 | ✅ |
| 화면 | 내 공간 목록, 역할·인원수 표시, 공간 생성 창 | ✅ |
| 화면 | 왼쪽 공간 선택, 공간·활동 내부의 기능별 상단 메뉴 | ✅ |
| 화면 | 주소에 공간·메뉴 유지, 생성 후 해당 공간 진입 | ✅ |
| 화면 | 숫자 사용자 ID로 초대 보내기, 전송·성공·실패 상태 | ✅ |
| 화면 | 받은 초대함 UI (수락 · 거절) | ⏳ |
| 공간 | 공간 삭제 API, 멤버 조회·역할 변경·내보내기 | ⏳ |
| 프론트 임시 데이터 | 기록 작성·편집·검색, 할 일·담당자·완료 관리, 달력 | ✅ |
| 프론트 임시 데이터 | 활동 생성·완료·보관·참여자, 공간·활동별 기능 추가 | ✅ |
| 프론트 임시 데이터 | 여행 일정·저장 장소·준비물·원화 정산·투표 | ✅ |
| 백엔드 | 활동·기록·할 일·일정·여행 기능의 API와 영속화 | ⏳ |

## 현재 화면에서 할 수 있는 일

- **회원가입 / 로그인:** 이름, 로그인 아이디, 이메일, 휴대폰 번호, 비밀번호로 가입합니다. 가입 후 자동 로그인하며, 비밀번호 표시·숨기기와 요청 오류 안내를 제공합니다.
- **내 공간:** 서버에서 참여 중인 공간을 조회하고 공간 ID, 제목, 내 역할, 참여 인원수를 표시합니다. 모든 공간은 표 형태로 표시하며 왼쪽에서 공간을 선택할 수 있습니다.
- **공간 내부:** 왼쪽에서 공간을 선택하고 상단에서 홈·활동·기록·할 일·일정 등을 엽니다. 공간과 활동마다 설치한 기능만 메뉴에 표시합니다. 주소의 `space`, `event`, `view`, `record` 쿼리에 선택 위치를 유지합니다.
- **공간 생성:** 이름의 앞뒤 공백을 제거하고 1~50자를 확인합니다. 생성 후 공간 목록을 다시 불러오고 새 공간으로 이동합니다.
- **사용자 초대:** 현재 선택한 공간과 양의 정수 사용자 ID를 서버에 전달합니다. 공간 전환 시 입력·결과를 초기화하며, 전송 중 공간 전환을 막고 브라우저 뒤로 가기로 이동한 경우 이전 결과를 다른 공간에 표시하지 않습니다. 로그인 아이디나 이메일로 사용자를 검색하는 기능은 아직 없습니다.
- **요청 상태:** 공간 로딩, 생성·초대 처리 중, 성공·실패 안내를 표시합니다. 인증 만료 응답은 세션을 정리하고 로그인 화면으로 이동합니다.

초대 수락·거절과 보낸·받은 초대 조회는 **백엔드 API만 구현**되어 있으며 프론트엔드 초대함은 아직 없습니다. 역할 값은 표시하지만 멤버 관리 화면이나 역할별 초대 제한은 구현하지 않았습니다.

### 브라우저 데이터로 동작하는 세부 기능

- **기록:** 제목·본문 작성과 수정, 검색, 공간/활동 범위 필터, 수동 저장. 저장하지 않은 내용이 있으면 공간 이동·로그아웃 전에 확인합니다.
- **할 일·일정:** 담당자·마감일·완료 필터, 종일/시간 약속, 월별 달력과 오늘 이동. 공간 달력에서 활동 기간도 확인합니다.
- **활동:** 여행·모임·스터디·직접 구성 유형으로 만들고 기능을 선택합니다. 날짜 미정도 가능하며, 완료·보관·보관 해제를 제공합니다. 보관한 활동은 읽기 전용입니다.
- **여행:** 날짜별 일정과 저장 장소 연결, 주소·선택 좌표 직접 입력, 준비물 체크와 담당자, 지출과 원화 균등 분담, 참여자 잔액과 정산 제안, 투표 생성·선택 변경·마감.
- **기능 추가·참여자:** 공간/활동별 기능 설치와 활동의 체험 참여자 선택. 실제 서버 멤버와 예시 인물을 구분합니다. 지출에 사용된 참여자는 제거할 수 없습니다.
- **향후 예시:** AI 추천과 공동 편집은 설명 화면만 제공합니다. 실제 AI 호출이나 여러 사용자 동시 편집은 없습니다.

세부 데이터는 로그인 아이디·공간 ID별 `localStorage`에 저장되어 새로고침 후 유지됩니다. 다른 브라우저·기기·사용자와 공유되지 않으며, 브라우저 데이터를 지우면 없어집니다. 여러 탭 간 동기화와 서버 업로드·자동 이관은 아직 없습니다. 장소 화면은 저장 목록과 좌표 안내이며 지도 검색 API를 연결하지 않았습니다. 정산은 계산 결과로 실제 송금을 실행하지 않습니다.

## 서비스 방향과 디자인 자료

지속적으로 유지하는 **Space(공간)** 안에 여행·모임 등 특정 활동인 **Event(활동)** 를 두고 필요한 기능을 선택합니다. 활동과 기능 모듈은 프론트엔드 임시 데이터로 구현했으며, 관련 서버 API는 다음 단계입니다.

| 기획 요소 | 현재 상태 |
| --- | --- |
| 활동 생성·완료·보관 | 프론트 임시 데이터로 동작 |
| 기록·할 일·일정, 선택형 기능 추가 | 프론트 임시 데이터로 동작 |
| 여행 일정·저장 장소·준비물·정산·투표 | 프론트 임시 데이터로 동작; 실제 지도 API는 예정 |
| 기록을 분석한 AI 기능 추천 | 향후 기능 예시 |
| 실시간 공동 편집 | 향후 기능 예시 |

- [PC 낮 테마 32장과 화면별 동작](docs/mockups/v3-pc-light/README.md)
- [32장 상호작용 설명](docs/mockups/v3-pc-light/interaction-guide.md)
- [간결한 디자인과 공간 상단 탭 변경 시안](docs/mockups/v4-clean-nav/README.md)
- [세부 기능 승인 시안 14장](docs/mockups/v5-local-features/README.md)
- [세부 기능 실제 구현 화면 17장과 검증 결과](docs/mockups/v5-local-features/implementation.md)
- [임시 데이터 구조와 백엔드 연결 준비](docs/architecture/frontend-local-content.md)

목업에는 예시 데이터와 예정 기능이 포함되어 있습니다. **목업 PNG는 동작하는 제품 화면이나 기능 구현 증거가 아닙니다.** 간결한 디자인과 공간 상단 메뉴는 승인 후 제품에 반영했습니다. 로그인·회원가입은 중앙 폼, 공간 목록은 표 형태로 정리하고 그라데이션·장식 이미지·동작 없는 비밀번호 찾기를 제거했습니다. 현재 없는 기능은 메뉴에 넣지 않습니다.

[실제 프론트 화면 캡처와 검증 결과](docs/mockups/v4-clean-nav/verification.md)를 확인할 수 있습니다. 화면 데이터는 검증용 모의 API 응답이며 실제 운영 데이터가 아닙니다.

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
│       ├── features/content     세부 기능 화면·모델·저장소·검증 계산
│       └── router               라우팅과 인증 가드
└── docs
    ├── architecture             기술 스택 문서
    ├── database                 ERD
    ├── devlog                   개발 기록
    └── mockups                  디자인 시안·PNG·상호작용 설명 (예정 기능 포함)
```

## 테스트

```bash
# 백엔드 — 실제 PostgreSQL 연결이 필요합니다
cd backend
export DB_PASSWORD="본인의 PostgreSQL 비밀번호"
./gradlew test

# 프론트엔드
cd frontend
npm run build # 타입 검사와 프로덕션 빌드
npm run test:unit # 정산·날짜·저장소·오류 처리
npm run test:e2e
```

프론트엔드 E2E 테스트에는 모의 API 응답을 사용하는 화면·요청 계약 검사가 포함되어 있습니다. 통과하더라도 실제 PostgreSQL과 백엔드가 연결된 전체 흐름의 검증을 대신하지 않습니다.

2026-10-05 세부 기능 구현 검증: 타입 검사·빌드 통과, 단위 검사 14개 통과, Chromium 화면 검사 19개 통과. 백엔드 코드는 이번 작업에서 변경하지 않았으며 실제 DB 통합 검사는 실행하지 않았습니다.

## 문서

- [기술 스택](docs/architecture/stack.md)
- [ERD](docs/database/ERD.puml)
- [개발 기록](docs/devlog)

## 라이선스

[MIT](LICENSE)
