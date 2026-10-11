import { footprints, validRoom, type Furniture, type FurnitureKind } from './model.ts'

export type RoomLayout = {
  schemaVersion: 1
  grid: { width: 8; height: 8 }
  elements: {
    id: string
    kind: FurnitureKind
    x: number
    y: number
    rotation: number
    module: string
    label: string
  }[]
}
export type RoomResponse = {
  spaceId: number
  version: number
  layout: RoomLayout
  updatedBy: number | null
  updatedAt: string | null
}
type Binding = { event: string; module: string; label: string }
type Bindings = Record<string, Binding>
export type DraftBackup = { version: number; furniture: Furniture[] }
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/

export function toLayout(furniture: Furniture[]): RoomLayout {
  if (!validRoom(furniture) || furniture.some((item) => !uuid.test(item.id)))
    throw new Error('가구 배치를 확인해주세요.')
  return {
    schemaVersion: 1,
    grid: { width: 8, height: 8 },
    elements: furniture.map(({ id, kind, x, y, rotation, module, label, event }) => ({
      id,
      kind,
      x,
      y,
      rotation,
      module: event ? '' : module,
      label: event ? '' : label,
    })),
  }
}

export function readResponse(value: unknown, spaceId: number): RoomResponse {
  const response = value as RoomResponse | null
  if (
    !response ||
    response.spaceId !== spaceId ||
    !Number.isSafeInteger(response.version) ||
    response.version < 0 ||
    response.layout?.schemaVersion !== 1 ||
    response.layout.grid?.width !== 8 ||
    response.layout.grid?.height !== 8 ||
    !Array.isArray(response.layout.elements)
  )
    throw new Error('서버 배치 형식을 확인할 수 없어요.')
  fromLayout(response.layout)
  return response
}

export function fromLayout(layout: RoomLayout, bindings: Bindings = {}): Furniture[] {
  const furniture = layout.elements.map((item) => {
    if (!item || !uuid.test(item.id) || !Object.hasOwn(footprints, item.kind))
      throw new Error('서버 배치 형식을 확인할 수 없어요.')
    const size = footprints[item.kind]
    const [w, h] = item.rotation % 2 ? [size[1], size[0]] : size
    // A private binding is applicable only while the shared furniture is decorative.
    const binding = item.module === '' && item.label === '' ? bindings[item.id] : undefined
    return { ...item, w, h, ...binding }
  })
  if (!validRoom(furniture)) throw new Error('서버 배치 형식을 확인할 수 없어요.')
  return furniture
}

export class RoomLocalData {
  readonly originalKey: string
  readonly bindingsKey: string
  readonly draftKey: string
  private storage: Pick<Storage, 'getItem' | 'setItem' | 'removeItem'>
  constructor(
    owner: string,
    spaceId: number,
    storage: Pick<Storage, 'getItem' | 'setItem' | 'removeItem'>,
  ) {
    this.storage = storage
    const suffix = `${encodeURIComponent(owner)}:${spaceId}`
    this.originalKey = `sai:room:v1:${suffix}`
    this.bindingsKey = `sai:room-bindings:v1:${suffix}`
    this.draftKey = `sai:room-draft:v1:${suffix}`
  }
  bindings(): Bindings {
    const raw = this.storage.getItem(this.bindingsKey)
    if (!raw) return {}
    const value = JSON.parse(raw) as { schemaVersion: number; bindings: Bindings }
    if (
      value.schemaVersion !== 1 ||
      !value.bindings ||
      typeof value.bindings !== 'object' ||
      Array.isArray(value.bindings)
    )
      throw new Error('활동 연결 정보를 읽지 못했어요. 원본은 보존됩니다.')
    for (const [id, b] of Object.entries(value.bindings)) {
      if (
        !uuid.test(id) ||
        !b ||
        typeof b.event !== 'string' ||
        typeof b.module !== 'string' ||
        typeof b.label !== 'string' ||
        !validRoom([{ id, kind: 'cat', x: 0, y: 0, rotation: 0, w: 1, h: 1, ...b }])
      )
        throw new Error('활동 연결 정보를 읽지 못했어요. 원본은 보존됩니다.')
    }
    return value.bindings
  }
  original(): Furniture[] | null {
    const raw = this.storage.getItem(this.originalKey)
    if (!raw) return null
    const items: unknown = JSON.parse(raw)
    if (!validRoom(items)) throw new Error('기존 배치를 읽지 못했어요. 원본은 보존됩니다.')
    toLayout(items)
    return items
  }
  draft(): DraftBackup | null {
    const raw = this.storage.getItem(this.draftKey)
    if (!raw) return null
    const backup = JSON.parse(raw) as DraftBackup
    if (!Number.isSafeInteger(backup.version) || backup.version < 0)
      throw new Error('복구 초안 형식이 올바르지 않아요.')
    toLayout(backup.furniture)
    return backup
  }
  prepare(version: number, furniture: Furniture[]) {
    // Validate the existing sidecar before any PUT; never silently overwrite corrupt data.
    this.bindings()
    toLayout(furniture)
    this.storage.setItem(this.draftKey, JSON.stringify({ version, furniture }))
  }
  finalize(furniture: Furniture[]) {
    const bindings = this.bindings()
    for (const item of furniture) {
      if (item.event)
        bindings[item.id] = { event: item.event, module: item.module, label: item.label }
      else delete bindings[item.id]
    }
    this.storage.setItem(this.bindingsKey, JSON.stringify({ schemaVersion: 1, bindings }))
    this.storage.removeItem(this.draftKey)
  }
  archive(version: number, furniture: Furniture[]) {
    this.storage.setItem(
      `${this.draftKey}:backup:${crypto.randomUUID()}`,
      JSON.stringify({ version, furniture }),
    )
  }
}
