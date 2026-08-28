import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { apiRequest } from '@/services/api'

export type SpaceRole = 'OWNER' | 'MANAGER' | 'MEMBER'

export type ParticipatingSpace = {
  spaceId: number
  title: string
  role: SpaceRole
  spaceMemberCount: number
}

type CreateSpaceResponse = { spaceId: number }

export const useSpacesStore = defineStore('spaces', () => {
  const spaces = ref<ParticipatingSpace[]>([])
  const loading = ref(false)
  const creating = ref(false)
  const inviting = ref(false)

  const hasSpaces = computed(() => spaces.value.length > 0)

  async function fetchMySpaces() {
    loading.value = true
    try {
      spaces.value = await apiRequest<ParticipatingSpace[]>('/api/spaces/my')
    } finally {
      loading.value = false
    }
  }

  async function createSpace(title: string) {
    creating.value = true
    try {
      const response = await apiRequest<CreateSpaceResponse>('/api/spaces', {
        method: 'POST',
        body: JSON.stringify({ title }),
      })
      await fetchMySpaces()
      return response.spaceId
    } finally {
      creating.value = false
    }
  }

  async function inviteUser(spaceId: number, inviteeUserId: number) {
    inviting.value = true
    try {
      return await apiRequest<number>('/api/invitations', {
        method: 'POST',
        body: JSON.stringify({ spaceId, inviteeUserId }),
      })
    } finally {
      inviting.value = false
    }
  }

  function clearSpaces() {
    spaces.value = []
  }

  return {
    spaces,
    loading,
    creating,
    inviting,
    hasSpaces,
    fetchMySpaces,
    createSpace,
    inviteUser,
    clearSpaces,
  }
})
