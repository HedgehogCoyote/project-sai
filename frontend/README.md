# SAI 프론트엔드

관계별 공간을 만들고, 필요한 도구를 3D 가구와 연결하는 Vue 3 프론트엔드입니다. 소개 → 로그인·회원가입 → 내 공간 → 방·이벤트·도구로 이동합니다.

## 실행

Node.js `^22.18.0 || >=24.12.0`을 사용합니다.

```bash
npm install
npm run dev
```

실제 백엔드 연결은 `http://localhost:5173`으로 접속합니다. API 기본 주소는 `http://localhost:8080`이며 `VITE_API_BASE_URL`로 변경할 수 있습니다. 다른 프론트 주소를 사용하려면 백엔드 CORS 설정도 확인해야 합니다.

## 기능과 저장

- 회원가입·로그인·로그아웃, 공간 목록·생성, 숫자 사용자 ID로 초대: 기존 백엔드 API 사용.
- 따뜻한 3D 방: 기본 가구 추가, 8 × 8 격자 드래그, 90도 회전, 충돌 검사, 저장·취소, 낮/저녁 조명.
- 가구 연결: 설치한 공간 도구 또는 이벤트의 도구로 이동. 장식으로만 사용할 수도 있습니다.
- 이벤트·기록·일반 할 일·달력·여행 일정·장소·준비물·정산·투표: 계정·공간별 브라우저 저장.
- 반복 할 일·Foot Print는 현재 범위에서 제외합니다.

가구와 세부 도구는 아직 다른 멤버·기기에 동기화되지 않습니다. 실제 사용자 검색 화면, 받은 초대함, 멤버 관리도 후속 범위입니다. [저장 구조와 렌더링](src/features/room/README.md), [전체 실행·API 설명](../README.md)을 참고하세요.

## 검증

```bash
npm run build
npm run test:unit
npm run test:e2e -- --project=chromium
```

3D를 포함한 Chromium 검사는 서버와 테스트를 별도 터미널에서 실행합니다.

```bash
# 터미널 1
npm run dev -- --host 127.0.0.1 --port 5193

# 터미널 2
npx playwright install chromium # 최초 1회
npx playwright test --config playwright.cottage.config.ts
```

E2E는 모의 API로 요청 계약과 화면을 검사합니다. 실제 DB 통합 검증을 대신하지 않으며, 3D 검사에서는 소프트웨어 WebGL을 사용합니다. 실제 모바일 GPU 성능은 별도 확인이 필요합니다. 3D 엔진은 지연 로딩하며 Vite의 번들 크기 경고가 남아 있습니다.

## 독립 목업

저장소 루트에서 실행합니다. Python이 필요합니다.

```bash
python -m http.server 5192 --bind 127.0.0.1 --directory docs/mockups/room-interactive
```

`http://127.0.0.1:5192`에서 로그인 없이 예시 공간을 체험합니다. [목업 기능과 제한](../docs/mockups/room-interactive/README.md)을 참고하세요. 실제 계정과 목업 데이터는 분리됩니다.

## 에셋

Three.js 0.184.0은 MIT, Kenney Furniture Kit 모델은 CC0입니다. 라이브러리는 `src/features/room/vendor`, 사용 모델은 `public/room3d/models`에 라이선스와 함께 보관합니다. 원본 게임 에셋은 사용하지 않습니다.
