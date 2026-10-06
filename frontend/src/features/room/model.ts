export type FurnitureKind =
  'sofa' | 'desk' | 'shelf' | 'plant' | 'rug' | 'frame' | 'cat' | 'calendar'
export type Furniture = {
  id: string
  kind: FurnitureKind
  x: number
  y: number
  w: number
  h: number
  rotation: number
  module: string
  label: string
  event?: string
}
export const furnitureNames: Record<FurnitureKind, string> = {
  sofa: '소파',
  desk: '책상',
  shelf: '선반',
  plant: '화분',
  rug: '러그',
  frame: '액자',
  cat: '고양이',
  calendar: '달력',
}
export const footprints: Record<FurnitureKind, [number, number]> = {
  sofa: [3, 2],
  desk: [2, 2],
  shelf: [2, 1],
  plant: [1, 1],
  rug: [3, 2],
  frame: [1, 1],
  cat: [1, 1],
  calendar: [1, 1],
}
export function fits(items: Furniture[], o: Furniture, x: number, y: number) {
  return (
    Number.isInteger(x) &&
    Number.isInteger(y) &&
    x >= 0 &&
    y >= 0 &&
    x + o.w <= 8 &&
    y + o.h <= 8 &&
    !items.some(
      (t) =>
        t.id !== o.id &&
        o.kind !== 'rug' &&
        t.kind !== 'rug' &&
        x < t.x + t.w &&
        x + o.w > t.x &&
        y < t.y + t.h &&
        y + o.h > t.y,
    )
  )
}
export function createFurniture(kind: FurnitureKind): Furniture {
  const [w, h] = footprints[kind]
  return { id: crypto.randomUUID(), kind, w, h, x: 0, y: 0, rotation: 0, module: '', label: '' }
}
export function firstFree(items: Furniture[], o: Furniture) {
  for (let y = 0; y < 8; y++) for (let x = 0; x < 8; x++) if (fits(items, o, x, y)) return { x, y }
  return null
}
export function seedRoom(modules: string[], expenseEvent?: string): Furniture[] {
  return (
    [
      ['sofa', 0, 4, 'tasks'],
      ['shelf', 5, 0, 'activities'],
      ['desk', 4, 3, expenseEvent ? 'expenses' : ''],
      ['plant', 0, 0, ''],
      ['cat', 2, 6, ''],
      ['frame', 6, 6, 'records'],
    ] as const
  ).map(([kind, x, y, module]) => ({
    ...createFurniture(kind),
    x,
    y,
    module:
      module === 'activities' || (expenseEvent && module === 'expenses') || modules.includes(module)
        ? module
        : '',
    label: '',
    ...(kind === 'desk' && expenseEvent ? { event: expenseEvent } : {}),
  }))
}
export function validRoom(value: unknown): value is Furniture[] {
  if (!Array.isArray(value) || value.length > 64) return false
  const ids = new Set<string>(),
    accepted: Furniture[] = []
  for (const item of value) {
    if (
      !item ||
      typeof item !== 'object' ||
      typeof item.id !== 'string' ||
      ids.has(item.id) ||
      !(item.kind in footprints) ||
      !Number.isInteger(item.rotation) ||
      item.rotation < 0 ||
      item.rotation > 3 ||
      typeof item.module !== 'string' ||
      ![
        '',
        'activities',
        'records',
        'tasks',
        'calendar',
        'itinerary',
        'places',
        'packing',
        'expenses',
        'polls',
      ].includes(item.module) ||
      typeof item.label !== 'string' ||
      item.label.length > 24 ||
      (item.event !== undefined && typeof item.event !== 'string')
    )
      return false
    const base = footprints[item.kind as FurnitureKind]
    const [w, h] = item.rotation % 2 ? [base[1], base[0]] : base
    if (item.w !== w || item.h !== h || !fits(accepted, item, item.x, item.y)) return false
    ids.add(item.id)
    accepted.push(item)
  }
  return true
}
