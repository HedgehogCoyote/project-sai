import { onMounted, ref } from 'vue'
import type { ContentRepository } from './repository'
import { BrowserContentRepository } from './repository'
import { isConflict } from './serverRepository'
import type { ContentState } from './types'

export function useContent(
  owner: string,
  spaceId: number,
  name: string,
  repository: ContentRepository = new BrowserContentRepository(),
) {
  const shared = 'shared' in repository && repository.shared === true
  const data = ref<ContentState | null>(null),
    loading = ref(true),
    saving = ref(false),
    error = ref(''),
    notice = ref('')
  async function load() {
    loading.value = true
    error.value = ''
    try {
      data.value = await repository.load(owner, spaceId, name)
    } catch (e) {
      error.value = e instanceof Error ? e.message : '데이터를 읽지 못했습니다.'
    } finally {
      loading.value = false
    }
  }
  async function commit(mutate: (next: ContentState) => void): Promise<boolean> {
    if (!data.value || saving.value) return false
    error.value = ''
    notice.value = ''
    saving.value = true
    try {
      // A conflict means another member saved first: take their version and re-apply this change.
      for (let attempt = 0; ; attempt++) {
        const next = JSON.parse(JSON.stringify(data.value)) as ContentState
        mutate(next)
        try {
          await repository.save(owner, spaceId, next)
          data.value = next
          break
        } catch (e) {
          if (!shared || !isConflict(e) || attempt >= 2) throw e
          data.value = await repository.load(owner, spaceId, name)
        }
      }
      notice.value = shared ? '공간 멤버와 함께 저장했습니다.' : '이 브라우저에 저장했습니다.'
      return true
    } catch (e) {
      error.value = e instanceof Error ? e.message : '저장하지 못했습니다. 입력을 유지했습니다.'
      return false
    } finally {
      saving.value = false
    }
  }
  /** Pulls other members' changes without a loading screen; skipped while a save is running. */
  async function refresh() {
    if (!shared || saving.value || loading.value || !data.value) return
    try {
      const next = await repository.load(owner, spaceId, name)
      if (!saving.value) data.value = next
    } catch {
      /* keep the current view; the next save reports real problems */
    }
  }
  onMounted(load)
  return { data, loading, saving, error, notice, shared, load, commit, refresh }
}
