# 공간 맵 JSON 저장 API

2026-10-10 구현. 공간별 맵 요소를 PostgreSQL `space_room.layout`의 JSONB로 저장한다.
최초 구현 범위는 백엔드 API였으며, 2026-10-11 Vue 방 편집 화면의 [서버 연동](room-server-sync.md)을 추가했다.
활동 연결과 콘텐츠·개인 설정은 여전히 로컬이다. API 계약은 그대로 유지한다.

## API 계약

두 API는 로그인 세션 쿠키가 필요하다.

| 요청 | 권한 | 동작 |
| --- | --- | --- |
| `GET /api/spaces/{spaceId}/room` | 해당 공간의 멤버 | 배치·수정 버전·수정자·수정 시각 조회 |
| `PUT /api/spaces/{spaceId}/room` | 해당 공간의 OWNER 또는 MANAGER | 기대 버전이 일치하면 전체 맵 JSON 교체 |

미저장 공간은 빈 배치, `version: 0`, `updatedBy: null`, `updatedAt: null`을 반환한다.
조회는 데이터를 생성하지 않는다. 빈 배열을 저장해 가구를 모두 치우는 것도 가능하다.
미인증은 401, 비멤버 접근과 일반 멤버의 저장은 403이다. 존재하지 않는 공간에도 비멤버 접근과 동일한 403을 반환한다.

PUT 요청 예시:

```json
{
  "version": 0,
  "layout": {
    "schemaVersion": 1,
    "grid": { "width": 8, "height": 8 },
    "elements": [
      {
        "id": "9d980e35-7de2-4b6c-8faa-5ddc78a0c44a",
        "kind": "desk",
        "x": 4,
        "y": 3,
        "rotation": 0,
        "module": "tasks",
        "label": "할 일"
      }
    ]
  }
}
```

성공 응답은 `spaceId`, `version`, `layout`, `updatedBy`, `updatedAt`이다.
최초 저장은 버전 1이며 매 저장마다 1씩 증가한다. 다음 PUT에는 마지막 조회/저장 응답의 버전을 보낸다.
`layout.schemaVersion`은 JSON 형식 버전, 응답의 `version`은 동시 수정 검사용 버전이다.

## 검증과 저장

- 격자는 8 × 8, 요소는 최대 64개다.
- ID는 소문자 표준 UUID이며 배치 안에서 중복될 수 없다.
- 가구는 `sofa`, `desk`, `shelf`, `plant`, `rug`, `frame`, `cat`, `calendar`를 지원한다.
- 좌표는 정수, 회전은 0~3(90도 단위)이다. 가구 크기는 서버가 종류와 회전으로 계산한다.
- 방 밖 배치와 가구 간 겹침을 차단한다. 기존 프론트 규칙처럼 러그와 다른 가구는 겹칠 수 있다.
- 연결 기능은 빈 문자열(장식), `activities`, `records`, `tasks`, `calendar`, `itinerary`, `places`, `packing`, `expenses`, `polls`다.
- label은 필수 문자열이며 빈 문자열 또는 최대 24자다.
- 필수 값 누락·소수 좌표·숫자의 문자열 변환·정의되지 않은 필드를 거부한다. 잘못된 JSON/배치는 400이다.
- 활동 ID 연결은 이번 형식에서 지원하지 않는다. 기능 설치·활동 데이터는 아직 로컬이므로 여기서는 연결 기능 이름만 검증한다.
- 낮/저녁 개인 설정과 GLB 모델·재질·렌더러 객체는 맵 JSON에 포함하지 않는다.

Flyway V4가 공간 ID를 기본키로 갖는 `space_room` 테이블을 만든다. JSONB와 함께 버전, 수정자, 수정 시각을 저장한다.
최초 저장은 `INSERT ... ON CONFLICT DO NOTHING`, 이후 저장은 `UPDATE ... WHERE version = ?`를 사용한다.
기대 버전이 맞지 않으면 409 `ROOM_VERSION_CONFLICT`를 반환한다. 실패한 요청은 기존 JSON을 변경하지 않는다.
저장과 응답 조회는 하나의 트랜잭션으로 처리한다.

409를 받은 클라이언트는 편집 초안을 유지하고 최신 맵을 다시 조회해야 한다. 최신 버전으로 자동 재시도하면 다른 사람의 변경을 덮어쓸 수 있다.
기존 브라우저 배치의 자동 업로드·활동 ID 이관은 구현하지 않았다.

## 검증

`RoomIntegrationTest`는 실제 PostgreSQL의 별도 임시 스키마에 V1~V4를 적용하고 테스트 후 스키마를 삭제한다.
현재 프로젝트와 동일한 `localhost:5432/sai`, `postgres`, `DB_PASSWORD` 환경을 사용한다. 테스트 계정에는 스키마 생성 권한이 필요하다.

- JSONB 저장·재조회 및 다른 공간 멤버의 조회
- 미저장 조회 시 테이블에 쓰지 않는 것
- OWNER와 MANAGER 저장, MEMBER 저장 차단, 비멤버/미인증 접근 차단
- 잘못된 요청과 오래된 버전이 기존 배치를 유지하는 것
- 최초 저장과 기존 배치 수정 각각의 동시 요청에서 정확히 하나만 성공하는 것

`RoomLayoutValidatorTest`는 회전된 가구의 크기, 겹침과 러그 예외, ID/종류/기능/라벨 검증, 필수 값·형식 버전·격자·개수 제한을 검사한다.

```powershell
cd backend
.\gradlew.bat test
```
