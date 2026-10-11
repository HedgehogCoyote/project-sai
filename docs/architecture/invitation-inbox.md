# 받은 초대함

2026-10-11. 승인된 `docs/mockups/invitation-inbox` 시안에 따라 구현했다.

## 동작과 데이터 경계

- 로그인한 화면의 헤더에서 받은 초대함(`/spaces?panel=invitations`)에 진입한다. 기존 `view=invite`는 공간에서 초대를 보내는 화면이다.
- 대기 중 / 처리 완료 탭을 제공한다. 배지는 확인된 PENDING 개수이며 조회 중·실패에는 숫자를 표시하지 않는다.
- GET `/api/invitations/received`의 공간 이름·초대한 사람·상태를 사용한다. 장식용 원형 그림은 공간 미리보기가 아니다. 초대 날짜·만료·페이지네이션을 추가하지 않았다.
- 수락은 POST `/api/invitations/join`, 거절은 확인 창을 거쳐 POST `/api/invitations/deny`에 invitationId를 보낸다. 수락 응답의 멤버 ID를 공간 ID로 사용하지 않는다.
- 수락 후 초대함을 유지한다. GET `/api/spaces/my`에서 실제 참여를 확인한 뒤 명시적으로 공간에 이동한다. 기존 공간의 미저장 콘텐츠·맵 이탈 확인을 유지한다.
- 이미 참여 중인 공간의 PENDING 초대는 수락 대신 공간 이동과 해당 초대 거절을 제공한다. 초대 거절은 멤버 자격을 제거하지 않는다.
- 서버의 가구 배치는 함께 보지만 활동·도구 콘텐츠는 기존 브라우저 로컬 저장 경계를 유지한다.

## 실패와 경합

처리 중에는 모든 초대 처리·새로고침·헤더 이동·로그아웃을 잠근다. 라우터 이탈을 막고 브라우저 이탈에는 beforeunload 확인을 요청한다. 브라우저 자체 종료를 보장해서 막을 수는 없으므로 재진입 때 서버 목록을 조회한다.

POST 실패 뒤 초대 목록과 참여 공간을 다시 확인한다. 두 조회가 확인되기 전 처리 버튼을 활성화하지 않는다. 자동 POST 재전송은 하지 않는다. 성공한 POST 뒤 GET만 실패하면 완료 안내를 유지하고 조회만 재시도한다. 다른 탭에서 처리된 경우 서버 메시지와 최신 상태를 표시한다.

계정이 바뀌면 저장소 상태와 요청 세대를 초기화한다. 이전 계정의 응답은 반영하지 않는다. 401은 기존 로그인 복귀 경로를 사용한다. 초대 ID·공간 ID·상태가 유효하지 않은 응답은 처리하지 않고 조회 실패로 표시한다. 서버 문자열은 Vue 텍스트 보간으로 렌더링한다.

## 시안과 차이

기존 앱 헤더·배경·여백을 사용한다. 모바일에서는 헤더의 계정 이름을 숨겨 받은 초대와 로그아웃 버튼을 유지한다. 시안의 두 초대 예시는 실제 API 결과로 대체되며 예시 안내 문구는 앱에 넣지 않는다. 거절 확인은 native dialog로 키보드 포커스와 Escape·취소 복귀를 지원한다.

## 검증

프런트 Chromium E2E는 **모의 API**로 Vue 화면과 요청·오류 처리를 검사한다. `implementation/desktop.png`와 `mobile.png`는 실행 중인 Vue 화면을 모의 API로 캡처한 결과이며 독립 HTML 목업과 구분한다.

백엔드 기존 `SpaceInvitationControllerTest`, `SpaceInvitationServiceTest`, `RoomIntegrationTest`도 실행했다. RoomIntegrationTest의 실제 HTTP 쿠키 세션·PostgreSQL 검증은 새 Vue 초대함과 실제 서버를 동시에 연결한 브라우저 검사와는 별개다. 실제 두 계정 브라우저 통합은 이번 검증에 포함하지 않았다.

실행 명령:

```powershell
# frontend
npm run build
npm run test:unit
npx eslint src/stores/invitations.ts src/components/InvitationInbox.vue src/views/HomeView.vue e2e/invitation-inbox.spec.ts
npx oxlint src/stores/invitations.ts src/components/InvitationInbox.vue src/views/HomeView.vue e2e/invitation-inbox.spec.ts
$env:CI='1'
npx playwright test --project=chromium --workers=1 --retries=0 --reporter=line

# backend
.\gradlew.bat test --tests '*SpaceInvitation*Test' --tests '*RoomIntegrationTest'
```

검증 결과: 빌드·타입 검사, 변경 파일 ESLint·Oxlint, 단위 테스트 24개, 초대함 E2E 13개, 기존 Chromium E2E 46개, 백엔드 관련 테스트 21개 통과. 첫 전체 실행은 기존 46개와 초대함 9개가 통과하고 401 테스트의 초기 조회 대기 누락으로 1개 실패했다. 대기 조건을 수정하고 경합·유효성 테스트 3개를 추가한 뒤 초대함 13개를 다시 실행했다.

전체 `npm run lint`는 기존 repository.ts·engine.js·Three.js vendor의 오류로 실패했다. 자동 수정된 vendor 파일은 되돌렸으며 이번 변경 파일의 ESLint·Oxlint 검사는 별도로 통과했다. 기존 606.6KB 엔진 번들 경고가 남아 있다.
