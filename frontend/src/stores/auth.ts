import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { apiRequest } from '@/services/api'

export type User = { email: string; name: string; phoneNumber: string; loginId: string }
export type SignupPayload = User & { password: string }

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const initialized = ref(false)
  const pending = ref(false)
  const isLoggedIn = computed(() => user.value !== null)

  async function fetchMe() {
    try { user.value = await apiRequest<User>('/api/auth/me') }
    catch { user.value = null }
    finally { initialized.value = true }
  }

  async function login(loginId: string, password: string) {
    pending.value = true
    try {
      await apiRequest<{ userId: number }>('/api/auth/login', { method: 'POST', body: JSON.stringify({ loginId, password }) })
      await fetchMe()
    } finally { pending.value = false }
  }

  async function signup(payload: SignupPayload) {
    pending.value = true
    try {
      await apiRequest<number>('/api/auth/signup', { method: 'POST', body: JSON.stringify(payload) })
      await apiRequest<{ userId: number }>('/api/auth/login', { method: 'POST', body: JSON.stringify({ loginId: payload.loginId, password: payload.password }) })
      await fetchMe()
    } finally { pending.value = false }
  }

  async function logout() {
    pending.value = true
    try { await apiRequest<void>('/api/auth/logout', { method: 'POST' }); user.value = null }
    finally { pending.value = false }
  }

  return { user, initialized, pending, isLoggedIn, fetchMe, login, signup, logout }
})
