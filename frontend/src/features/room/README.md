# SAI 3D room

`RoomBoard.vue` manages account/space-specific furniture, grid placement, rotation, tool bindings and day/evening selection. `RoomCanvas.vue` loads `engine.js` only when the space room is opened. Three.js and the GLTF loader are compiled by Vite; public GLB models are resolved using Vite's configured base URL.

Furniture uses the existing GET/PUT `/api/spaces/{spaceId}/room` API. OWNER/MANAGER may edit; MEMBER may view. Empty rooms stay empty. Old `sai:room:v1:<encoded-loginId>:<spaceId>` layouts can be explicitly imported into a draft; they are never uploaded automatically or deleted. Private activity bindings use account/space/furniture-scoped `sai:room-bindings:v1` storage. A write-ahead `sai:room-draft:v1` record preserves private bindings across interrupted saves; discarded conflict drafts are archived under `:backup:<uuid>` keys. These archived drafts can currently be inspected in browser storage, without a history-picker UI. Day/evening remains local. Failed saves keep drafts; 409 blocks saving and supports latest-layout inspection, with confirmation and backup before starting fresh. Unsaved changes use the existing route/logout confirmation flow, and navigation is blocked while saving. See [server integration](../../../../docs/architecture/room-server-sync.md).

Tools and events use the existing content repository and routes. Furniture can bind to an installed space tool, an event's installed tool, or the event list. Missing tool bindings do not discard content and prompt the user to add the tool. Recurring tasks and Foot Print are not introduced.

## Assets and rendering

- `vendor`: Three.js 0.184.0, MIT; LICENSE included. GLTFLoader utility imports and bare Three imports are adapted to local relative modules. No dependency install is required.
- `public/room3d/models`: 10 GLB files from Kenney Furniture Kit, CC0; LICENSE included. Source: https://kenney.nl/assets/furniture-kit
- Room walls, floor, window, lights, cat, frame and calendar use procedural geometry.
- Matte MeshStandardMaterial with a warm height-based fragment tint, ACES tone mapping, cream/peach key lighting, lavender fill and PCF soft shadows. Fixed orthographic camera and floor-plane raycasts map furniture to an 8 × 8 grid.
- Rendering is on demand. Renderer, materials and models are reused during edits; observers, listeners, shadow maps and generated geometry are disposed on view closure.

## Verification

2026-10-11: Type check/build and 24 unit tests passed. The complete Chromium suite passed 46 tests using stubbed API responses, including room permissions, import, conflicts, interrupted-save recovery and desktop/mobile layouts. Backend tests passed 58 cases, including two real HTTP cookie sessions with PostgreSQL for signup/search/invite/join/shared-room read, MEMBER write denial and stale-version conflicts. This is not a live Vue-to-server browser E2E. Actual mobile GPU performance and real-time synchronization are not tested. See [verification evidence](../../../../docs/architecture/room-server-sync.md).

The engine is a lazy chunk (~607 kB minified, ~157 kB gzip), excluded from the initial login/landing load. Three.js size currently causes Vite's generic 500 kB chunk warning.

Start the normal dev server, or `node node_modules/vite/bin/vite.js --host 127.0.0.1 --port 5193`. With this server running, `node node_modules/@playwright/test/cli.js test --config playwright.cottage.config.ts` runs Chromium headlessly with software WebGL.
