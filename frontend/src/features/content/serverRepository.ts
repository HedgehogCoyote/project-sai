import { ApiError } from '@/services/api'
import { api } from '@/services/workspace'
import type { ContentRepository } from './repository'
import { contentKey } from './repository'
import type { ContentState } from './types'
import { describeChange, emptyContent, mapMemberIds, memberId, mergeMembers, normalize } from './contentSync'
export { describeChange, emptyContent, mapMemberIds, memberId, mergeMembers, normalize }

export function isConflict(e: unknown) {
  return e instanceof ApiError && e.status === 409
}

/**
 * Content shared with every member through the server. Saves carry the version they
 * were based on; on 409 `useContent` reloads and re-applies the same change.
 */
export class ServerContentRepository implements ContentRepository {
  readonly shared = true
  private version = 0
  private last: ContentState | null = null

  constructor(
    private readonly spaceId: number,
    private readonly selfUserId: number,
  ) {}

  private toLocal = (id: string) => (id === memberId(this.selfUserId) ? 'self' : id)
  private toServer = (id: string) => (id === 'self' ? memberId(this.selfUserId) : id)

  async load(): Promise<ContentState> {
    const [res, members] = await Promise.all([
      api.content<ContentState>(this.spaceId),
      api.members(this.spaceId),
    ])
    this.version = res.version
    const state = mapMemberIds(normalize(res.content ?? emptyContent()), this.toLocal)
    state.members = mergeMembers(state.members, members, this.selfUserId)
    this.last = JSON.parse(JSON.stringify(state))
    return state
  }

  async save(_owner: string, _spaceId: number, state: ContentState): Promise<void> {
    const change = describeChange(this.last, state)
    const res = await api.saveContent(this.spaceId, {
      version: this.version,
      content: mapMemberIds(state, this.toServer),
      ...change,
    })
    this.version = res.version
    this.last = JSON.parse(JSON.stringify(state))
  }

  /** Demo or earlier data saved in this browser before content was shared. */
  localDraft(owner: string): ContentState | null {
    try {
      const raw = localStorage.getItem(contentKey(owner, this.spaceId))
      return raw ? normalize(JSON.parse(raw) as ContentState) : null
    } catch {
      return null
    }
  }
}
