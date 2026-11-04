/** Souvenirs a Foot Print can leave in the room. Photo holders show the Foot Print's own photo. */
export type SouvenirDef = { id: string; label: string; photo?: boolean; tags: string[] }
export const SOUVENIRS: SouvenirDef[] = [
  { id: 'polaroid', label: '폴라로이드', photo: true, tags: ['travel', 'meeting', 'study', 'custom'] },
  { id: 'frame', label: '사진 액자', photo: true, tags: ['travel', 'meeting', 'study', 'custom'] },
  { id: 'acrylic', label: '아크릴 스탠드', photo: true, tags: ['travel', 'meeting', 'custom'] },
  { id: 'shell', label: '조개', tags: ['travel'] },
  { id: 'ticket', label: '기차표', tags: ['travel'] },
  { id: 'lantern', label: '캠핑 랜턴', tags: ['travel', 'meeting'] },
  { id: 'cake', label: '케이크 모형', tags: ['meeting'] },
  { id: 'trophy', label: '트로피', tags: ['study', 'meeting'] },
  { id: 'medal', label: '메달', tags: ['study', 'meeting'] },
  { id: 'ball', label: '축구공', tags: ['meeting', 'custom'] },
  { id: 'jar', label: '추억 유리병', tags: ['travel', 'meeting', 'study', 'custom'] },
  { id: 'plant', label: '같이 키운 화분', tags: ['custom', 'meeting'] },
]
export const souvenirLabel = (id: string) => SOUVENIRS.find((s) => s.id === id)?.label ?? id
export const souvenirIsPhoto = (id: string) => !!SOUVENIRS.find((s) => s.id === id)?.photo
/** Recommended first for an activity type, the rest after. */
export function souvenirsFor(type: string): SouvenirDef[] {
  return [...SOUVENIRS].sort((a, b) => Number(b.tags.includes(type)) - Number(a.tags.includes(type)))
}
/** "폴라로이드가", "조개가", "기차표가" vs "케이크 모형이": picks 이/가 by the last syllable. */
export function withSubject(word: string) {
  const c = word.charCodeAt(word.length - 1)
  return word + (c >= 0xac00 && c <= 0xd7a3 && (c - 0xac00) % 28 ? '이' : '가')
}
