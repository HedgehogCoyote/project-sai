import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { ApiError, apiRequest } from '@/services/api'
import { useAuthStore } from './auth'
import { useSpacesStore, type ParticipatingSpace } from './spaces'

export type ReceivedInvitation = {
  invitationId: number
  spaceId: number
  spaceName: string
  inviterName: string
  invitationStatus: 'PENDING' | 'ACCEPTED' | 'DENIED'
}
function validInvitation(value: unknown): value is ReceivedInvitation {
  if (!value || typeof value !== 'object') return false
  const item = value as ReceivedInvitation
  return (
    Number.isSafeInteger(item.invitationId) &&
    item.invitationId > 0 &&
    Number.isSafeInteger(item.spaceId) &&
    item.spaceId > 0 &&
    typeof item.spaceName === 'string' &&
    typeof item.inviterName === 'string' &&
    ['PENDING', 'ACCEPTED', 'DENIED'].includes(item.invitationStatus)
  )
}
export const useInvitationsStore = defineStore('invitations', () => {
  const auth = useAuthStore()
  const spaces = useSpacesStore()
  const items = ref<ReceivedInvitation[]>([])
  const loading = ref(false)
  const processing = ref<number | null>(null)
  const confirmed = ref(false)
  const membershipConfirmed = ref(false)
  const unresolved = ref(false)
  const error = ref('')
  const notice = ref('')
  const acceptedSpaceId = ref<number | null>(null)
  const sessionError = ref<ApiError | null>(null)
  let generation = 0
  const pendingCount = computed(() =>
    confirmed.value ? items.value.filter((i) => i.invitationStatus === 'PENDING').length : null,
  )
  const busy = computed(() => loading.value || processing.value !== null)
  watch(
    () => auth.user?.loginId,
    () => {
      generation++
      items.value = []
      confirmed.value = false
      membershipConfirmed.value = false
      loading.value = false
      processing.value = null
      unresolved.value = false
      error.value = ''
      notice.value = ''
      acceptedSpaceId.value = null
      sessionError.value = null
    },
    { flush: 'sync' },
  )
  function report(cause: unknown, fallback: string) {
    if (cause instanceof ApiError && cause.status === 401) sessionError.value = cause
    return cause instanceof ApiError ? cause.message : fallback
  }
  async function refresh() {
    if (busy.value || !auth.user) return
    const token = ++generation
    loading.value = true
    error.value = ''
    confirmed.value = false
    membershipConfirmed.value = false
    const results = await Promise.allSettled([
      apiRequest<unknown>('/api/invitations/received'),
      apiRequest<ParticipatingSpace[]>('/api/spaces/my'),
    ])
    if (token !== generation) return
    const [invitations, memberships] = results
    if (
      invitations.status === 'fulfilled' &&
      Array.isArray(invitations.value) &&
      invitations.value.every(validInvitation)
    ) {
      items.value = invitations.value
      confirmed.value = true
    } else {
      error.value = report(
        invitations.status === 'rejected' ? invitations.reason : null,
        '초대 목록을 불러오지 못했습니다.',
      )
    }
    if (memberships.status === 'fulfilled' && Array.isArray(memberships.value)) {
      spaces.spaces = memberships.value
      membershipConfirmed.value = true
    } else {
      const message = report(
        memberships.status === 'rejected' ? memberships.reason : null,
        '공간 목록을 불러오지 못했습니다.',
      )
      error.value = [error.value, message].filter(Boolean).join(' ')
    }
    if (confirmed.value && membershipConfirmed.value) unresolved.value = false
    loading.value = false
  }
  function isMember(spaceId: number) {
    return membershipConfirmed.value && spaces.spaces.some((s) => s.spaceId === spaceId)
  }
  async function process(item: ReceivedInvitation, action: 'join' | 'deny') {
    if (
      busy.value ||
      unresolved.value ||
      !confirmed.value ||
      !membershipConfirmed.value ||
      !items.value.some(
        (i) => i.invitationId === item.invitationId && i.invitationStatus === 'PENDING',
      ) ||
      (action === 'join' && isMember(item.spaceId))
    )
      return
    const token = generation
    processing.value = item.invitationId
    error.value = ''
    notice.value = ''
    acceptedSpaceId.value = null
    try {
      await apiRequest<number>(`/api/invitations/${action}`, {
        method: 'POST',
        body: JSON.stringify({ invitationId: item.invitationId }),
      })
      if (token !== generation) return
      notice.value =
        action === 'join'
          ? `${item.spaceName} 공간 참여는 완료됐어요.`
          : `${item.spaceName} 초대를 거절했어요.`
      if (action === 'join') acceptedSpaceId.value = item.spaceId
    } catch (cause) {
      if (token !== generation) return
      error.value = report(cause, '처리 결과를 확인하지 못했습니다.')
      unresolved.value = true
    } finally {
      if (token === generation) processing.value = null
    }
    if (token !== generation || sessionError.value) return
    const failure = error.value
    unresolved.value = true
    const reconciliation = refresh()
    const reconciliationGeneration = generation
    await reconciliation
    if (generation !== reconciliationGeneration) return
    if (failure && !error.value) error.value = `${failure} 최신 초대 상태를 확인했어요.`
  }
  return {
    items,
    loading,
    processing,
    confirmed,
    membershipConfirmed,
    unresolved,
    error,
    notice,
    acceptedSpaceId,
    sessionError,
    pendingCount,
    busy,
    refresh,
    process,
    isMember,
  }
})
