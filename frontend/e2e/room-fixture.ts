import type { Page } from '@playwright/test'
import { seedRoom } from '../src/features/room/model'
import { toLayout, type RoomResponse } from '../src/features/room/server'

export async function mockRoomApi(page: Page, seeded = false) {
  const rooms = new Map<number, RoomResponse>()
  const furniture = seeded ? seedRoom(['tasks', 'records', 'calendar'], 'demo-trip') : []
  if (seeded)
    await page.addInitScript((items) => {
      const key = 'sai:room-bindings:v1:roomtester:1'
      if (!localStorage.getItem(key))
        localStorage.setItem(
          key,
          JSON.stringify({
            schemaVersion: 1,
            bindings: Object.fromEntries(
              items
                .filter((item) => item.event)
                .map((item) => [
                  item.id,
                  { event: item.event, module: item.module, label: item.label },
                ]),
            ),
          }),
        )
    }, furniture)
  await page.route('**/api/spaces/*/room', async (route) => {
    const spaceId = Number(new URL(route.request().url()).pathname.split('/')[3])
    let room = rooms.get(spaceId) ?? {
      spaceId,
      version: seeded ? 1 : 0,
      layout: toLayout(furniture),
      updatedBy: null,
      updatedAt: null,
    }
    if (route.request().method() === 'PUT') {
      const body = route.request().postDataJSON()
      if (body.version !== room.version)
        return route.fulfill({ status: 409, json: { code: 'ROOM_VERSION_CONFLICT' } })
      room = {
        ...room,
        layout: body.layout,
        version: room.version + 1,
        updatedAt: '2026-10-11T03:40:00Z',
      }
      rooms.set(spaceId, room)
    }
    await route.fulfill({ json: room })
  })
  return rooms
}
