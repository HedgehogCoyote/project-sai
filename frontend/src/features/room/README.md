# SAI 3D room

`RoomBoard.vue` manages account/space-specific furniture, grid placement, rotation, tool bindings and day/evening selection. `RoomCanvas.vue` loads `engine.js` only when the space room is opened. Three.js and the GLTF loader are compiled by Vite; public GLB models are resolved using Vite's configured base URL.

No backend endpoints are added. Authentication, space creation and invitations use the existing API. Furniture is stored at `sai:room:v1:<encoded-loginId>:<spaceId>` in localStorage, separate from content and the standalone mock. Mock example accounts and furniture are not migrated into real accounts. Failed saves keep the editing draft. Unsaved changes use the existing route/logout confirmation flow.

Tools and events use the existing content repository and routes. Furniture can bind to an installed space tool, an event's installed tool, or the event list. Missing tool bindings do not discard content and prompt the user to add the tool. Recurring tasks and Foot Print are not introduced.

## Assets and rendering

- `vendor`: Three.js 0.184.0, MIT; LICENSE included. GLTFLoader utility imports and bare Three imports are adapted to local relative modules. No dependency install is required.
- `public/room3d/models`: 10 GLB files from Kenney Furniture Kit, CC0; LICENSE included. Source: https://kenney.nl/assets/furniture-kit
- Room walls, floor, window, lights, cat, frame and calendar use procedural geometry.
- Matte MeshStandardMaterial with a warm height-based fragment tint, ACES tone mapping, cream/peach key lighting, lavender fill and PCF soft shadows. Fixed orthographic camera and floor-plane raycasts map furniture to an 8 × 8 grid.
- Rendering is on demand. Renderer, materials and models are reused during edits; observers, listeners, shadow maps and generated geometry are disposed on view closure.

## Verification

Type check and production build passed. 18 unit tests passed. Chromium API-contract/content/3D integration tests passed using stubbed API responses; this does not verify a running backend. 3D tests cover dragging, collision/boundary rejection, rotation and reload persistence, cancelled drafts, space isolation and furniture-to-event-tool routing. The extra room test checks desktop/mobile-width layouts. Actual mobile GPU performance and multi-user synchronization are not tested.

The engine is a lazy chunk (~607 kB minified, ~157 kB gzip), excluded from the initial login/landing load. Three.js size currently causes Vite's generic 500 kB chunk warning.

Start the normal dev server, or `node node_modules/vite/bin/vite.js --host 127.0.0.1 --port 5193`. With this server running, `node node_modules/@playwright/test/cli.js test --config playwright.cottage.config.ts` runs Chromium headlessly with software WebGL.
