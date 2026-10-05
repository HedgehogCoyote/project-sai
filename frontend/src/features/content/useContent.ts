import { onMounted, ref } from 'vue'
import type { ContentRepository } from './repository'
import { BrowserContentRepository } from './repository'
import type { ContentState } from './types'

export function useContent(
  owner: string,
  spaceId: number,
  name: string,
  repository: ContentRepository = new BrowserContentRepository(),
) {
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
      const next = JSON.parse(JSON.stringify(data.value)) as ContentState
      mutate(next)
      await repository.save(owner, spaceId, next)
      data.value = next
      notice.value = '이 브라우저에 저장했습니다.'
      return true
    } catch (e) {
      error.value = e instanceof Error ? e.message : '저장하지 못했습니다. 입력을 유지했습니다.'
      return false
    } finally {
      saving.value = false
    }
  }
  onMounted(load)
  return { data, loading, saving, error, notice, load, commit }
}
