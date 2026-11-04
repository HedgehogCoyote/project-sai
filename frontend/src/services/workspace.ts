import { apiRequest, BASE_URL } from './api'

export type Relation = 'COUPLE' | 'FRIENDS' | 'CLUB' | 'CUSTOM'
export type Role = 'OWNER' | 'MANAGER' | 'MEMBER'
export type SpaceDetail = {
  spaceId: number
  title: string
  relation: Relation
  colorPrimary: string
  colorSecondary: string
  myRole: Role
  memberCount: number
  muted: boolean
}
export type SpaceMember = {
  userId: number
  loginId: string
  name: string
  nickname: string | null
  role: Role
  joinedAt: string
}
export type InviteLink = { id: number; token: string; createdAt: string; expiresAt: string; createdByName: string }
export type LinkPreview = {
  status: 'VALID' | 'EXPIRED' | 'REVOKED'
  spaceId: number
  spaceTitle: string
  relation: Relation
  colorPrimary: string
  colorSecondary: string
  memberCount: number
  inviterName: string
  expiresAt: string
  alreadyMember: boolean
}
export type FeedItem = {
  id: number
  spaceId: number
  spaceTitle: string
  kind: string
  module: string | null
  summary: string
  actorId: number | null
  actorName: string
  createdAt: string
  unread: boolean
}
export type Unread = { spaceId: number; total: number; modules: Record<string, number> }
export type Overview = { spaceId: number; relation: Relation; colorPrimary: string; colorSecondary: string; unread: number; modules: Record<string, number>; muted: boolean }
export type Post = {
  id: number
  kind: 'NOTICE' | 'COMMENT'
  target: string | null
  body: string
  pinned: boolean
  authorId: number
  authorName: string
  createdAt: string
  mine: boolean
  reactions: Record<string, number>
  myReactions: string[]
}
export const EMOJIS = ['👍', '❤️', '😂', '😮', '🎉']
export type Photo = {
  id: number
  eventId: string | null
  caption: string | null
  uploaderId: number
  uploaderName: string
  contentType: string
  sizeBytes: number
  createdAt: string
  url: string
  mine: boolean
}
export type ContentResponse<T> = {
  version: number
  content: T | null
  updatedAt: string | null
  updatedBy: number | null
  updatedByName: string | null
}

const json = (body: unknown) => JSON.stringify(body)
const sp = (id: number) => `/api/spaces/${id}`

export const RELATIONS: { id: Relation; label: string; hint: string }[] = [
  { id: 'COUPLE', label: '연인', hint: '둘이서 · 초대받은 사람도 관리자' },
  { id: 'FRIENDS', label: '친구', hint: '여행과 모임 · 투표와 정산' },
  { id: 'CLUB', label: '동아리', hint: '정기 모임 · 공지와 운영진' },
  { id: 'CUSTOM', label: '직접 구성', hint: '필요한 기능을 직접 고름' },
]
/** Two-circle colour pairs; the first is the room's main colour, the second its accent. */
export const PALETTES: { label: string; primary: string; secondary: string }[] = [
  { label: '모브·자두', primary: '#aa849c', secondary: '#71576c' },
  { label: '살구·하늘', primary: '#e3976b', secondary: '#6c8db5' },
  { label: '세이지·흙', primary: '#9bb08a', secondary: '#c98b6b' },
  { label: '물빛·남색', primary: '#7c9cc1', secondary: '#3f5a7a' },
  { label: '이끼·숲', primary: '#b5b872', secondary: '#4f6b55' },
  { label: '노을·벽돌', primary: '#efb15e', secondary: '#a8574a' },
  { label: '라일락·민트', primary: '#b9a3d6', secondary: '#6fae9f' },
  { label: '모래·먹', primary: '#d6c09a', secondary: '#4b4a52' },
]
export const roleLabel = (r: Role) => ({ OWNER: '소유자', MANAGER: '관리자', MEMBER: '멤버' })[r]
export const relationLabel = (r: Relation) => RELATIONS.find((x) => x.id === r)?.label ?? r
export const fileUrl = (path: string) => BASE_URL + path

export const api = {
  space: (id: number) => apiRequest<SpaceDetail>(sp(id)),
  updateSpace: (id: number, body: Partial<Pick<SpaceDetail, 'title' | 'relation' | 'colorPrimary' | 'colorSecondary'>>) =>
    apiRequest<SpaceDetail>(sp(id), { method: 'PATCH', body: json(body) }),
  deleteSpace: (id: number) => apiRequest<void>(sp(id), { method: 'DELETE' }),
  leave: (id: number) => apiRequest<{ spaceDeleted: boolean }>(sp(id) + '/leave', { method: 'POST' }),
  members: (id: number) => apiRequest<SpaceMember[]>(sp(id) + '/members'),
  nickname: (id: number, nickname: string) =>
    apiRequest<void>(sp(id) + '/members/me', { method: 'PATCH', body: json({ nickname }) }),
  setRole: (id: number, userId: number, role: Role) =>
    apiRequest<void>(sp(id) + '/members/' + userId, { method: 'PATCH', body: json({ role }) }),
  removeMember: (id: number, userId: number) => apiRequest<void>(sp(id) + '/members/' + userId, { method: 'DELETE' }),
  links: (id: number) => apiRequest<InviteLink[]>(sp(id) + '/invite-links'),
  createLink: (id: number) => apiRequest<InviteLink>(sp(id) + '/invite-links', { method: 'POST' }),
  revokeLink: (id: number, linkId: number) =>
    apiRequest<void>(sp(id) + '/invite-links/' + linkId, { method: 'DELETE' }),
  previewLink: (token: string) => apiRequest<LinkPreview>('/api/invite-links/' + encodeURIComponent(token)),
  joinLink: (token: string) =>
    apiRequest<{ spaceId: number; alreadyMember: boolean }>('/api/invite-links/' + encodeURIComponent(token) + '/join', {
      method: 'POST',
    }),
  content: <T>(id: number) => apiRequest<ContentResponse<T>>(sp(id) + '/content'),
  saveContent: <T>(id: number, body: { version: number; content: T; summary?: string; module?: string }) =>
    apiRequest<ContentResponse<T>>(sp(id) + '/content', { method: 'PUT', body: json(body) }),
  feed: (limit = 40) => apiRequest<FeedItem[]>('/api/feed?limit=' + limit),
  spaceFeed: (id: number, limit = 30) => apiRequest<FeedItem[]>(sp(id) + '/feed?limit=' + limit),
  unread: () => apiRequest<Unread[]>('/api/feed/unread'),
  overview: () => apiRequest<Overview[]>('/api/spaces/overview'),
  seen: (id: number) => apiRequest<void>(sp(id) + '/seen', { method: 'POST' }),
  notices: (id: number) => apiRequest<Post[]>(sp(id) + '/notices'),
  postNotice: (id: number, body: string, pinned: boolean) =>
    apiRequest<Post>(sp(id) + '/notices', { method: 'POST', body: json({ body, pinned }) }),
  comments: (id: number, target: string) =>
    apiRequest<Post[]>(sp(id) + '/comments?target=' + encodeURIComponent(target)),
  commentCounts: (id: number) => apiRequest<Record<string, number>>(sp(id) + '/comment-counts'),
  postComment: (id: number, target: string, body: string) =>
    apiRequest<Post>(sp(id) + '/comments', { method: 'POST', body: json({ target, body }) }),
  react: (id: number, postId: number, emoji: string) =>
    apiRequest<Post>(sp(id) + '/posts/' + postId + '/reactions', { method: 'POST', body: json({ emoji }) }),
  mute: (id: number, muted: boolean) =>
    apiRequest<void>(sp(id) + '/members/me/notifications', { method: 'PATCH', body: json({ muted }) }),
  deletePost: (id: number, postId: number) => apiRequest<void>(sp(id) + '/posts/' + postId, { method: 'DELETE' }),
  photos: (id: number, eventId?: string | null) =>
    apiRequest<Photo[]>(sp(id) + '/photos' + (eventId ? '?eventId=' + encodeURIComponent(eventId) : '')),
  uploadPhoto: (id: number, file: File, eventId?: string | null, caption?: string) => {
    const form = new FormData()
    form.append('file', file)
    if (eventId) form.append('eventId', eventId)
    if (caption) form.append('caption', caption)
    return apiRequest<Photo>(sp(id) + '/photos', { method: 'POST', body: form })
  },
  updatePhoto: (id: number, photoId: number, caption: string, eventId: string | null) =>
    apiRequest<Photo>(sp(id) + '/photos/' + photoId, { method: 'PATCH', body: json({ caption, eventId }) }),
  deletePhoto: (id: number, photoId: number) => apiRequest<void>(sp(id) + '/photos/' + photoId, { method: 'DELETE' }),
  updateProfile: (name: string) => apiRequest<void>('/api/account/profile', { method: 'PATCH', body: json({ name }) }),
  changePassword: (currentPassword: string, newPassword: string) =>
    apiRequest<void>('/api/account/password', { method: 'POST', body: json({ currentPassword, newPassword }) }),
  deleteAccount: (password: string) => apiRequest<void>('/api/account', { method: 'DELETE', body: json({ password }) }),
}

export function timeAgo(iso: string, now = Date.now()): string {
  const diff = Math.max(0, now - Date.parse(iso)) / 1000
  if (diff < 60) return '방금'
  if (diff < 3600) return `${Math.floor(diff / 60)}분 전`
  if (diff < 86400) return `${Math.floor(diff / 3600)}시간 전`
  if (diff < 86400 * 7) return `${Math.floor(diff / 86400)}일 전`
  return new Intl.DateTimeFormat('ko-KR', { month: 'long', day: 'numeric' }).format(new Date(iso))
}
