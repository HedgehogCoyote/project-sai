<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ApiError, apiRequest } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { useSpacesStore, type ParticipatingSpace } from '@/stores/spaces'

type SearchUser = { userId: number; loginId: string; name: string }
const props = defineProps<{ space: ParticipatingSpace }>()
const emit = defineEmits<{ sessionExpired: [error: ApiError] }>()
const auth = useAuthStore()
const spaces = useSpacesStore()
const query = ref('')
const results = ref<SearchUser[]>([])
const selectedId = ref<number | null>(null)
const searching = ref(false)
const searched = ref(false)
const searchError = ref('')
const inviteError = ref('')
const inviteSuccess = ref('')
const keyword = computed(() => query.value.trim())
const validKeyword = computed(() => keyword.value.length >= 2 && keyword.value.length <= 20)
const selectedUser = computed(() => results.value.find((user) => user.userId === selectedId.value))
const isSelf = (user: SearchUser) => user.loginId === auth.user?.loginId
const canInvite = computed(() => !!selectedUser.value && !isSelf(selectedUser.value) && !searching.value)
let controller: AbortController | undefined
let requestId = 0
let alive = true

function resetSearch() {
  requestId++
  controller?.abort()
  controller = undefined
  searching.value = false
  searched.value = false
  results.value = []
  selectedId.value = null
  searchError.value = ''
  inviteError.value = ''
  inviteSuccess.value = ''
}
watch(query, resetSearch, { flush: 'sync' })
watch(() => props.space.spaceId, () => {
  query.value = ''
  resetSearch()
}, { flush: 'sync' })
onBeforeUnmount(() => {
  alive = false
  resetSearch()
})

async function search() {
  if (spaces.inviting || searching.value) return
  resetSearch()
  if (!validKeyword.value) {
    searchError.value = '검색어는 2~20자로 입력해 주세요.'
    return
  }
  const current = requestId
  const spaceId = props.space.spaceId
  controller = new AbortController()
  searching.value = true
  try {
    const users = await apiRequest<SearchUser[]>(`/api/users/search?loginId=${encodeURIComponent(keyword.value)}`, {
      signal: controller.signal,
    })
    if (!alive || current !== requestId || spaceId !== props.space.spaceId) return
    results.value = users
    searched.value = true
  } catch (error) {
    if (!alive || current !== requestId || spaceId !== props.space.spaceId) return
    if (error instanceof ApiError && error.status === 401) emit('sessionExpired', error)
    else searchError.value = error instanceof ApiError ? error.message : '검색하지 못했습니다. 다시 검색해 주세요.'
  } finally {
    if (current === requestId) {
      searching.value = false
      controller = undefined
    }
  }
}

async function invite() {
  if (!canInvite.value || spaces.inviting) return
  const target = selectedUser.value!
  const spaceId = props.space.spaceId
  inviteError.value = ''
  inviteSuccess.value = ''
  try {
    await spaces.inviteUser(spaceId, target.userId)
    if (!alive || props.space.spaceId !== spaceId) return
    inviteSuccess.value = `${target.name}(@${target.loginId}) 님에게 초대를 보냈습니다.`
    selectedId.value = null
  } catch (error) {
    if (!alive || props.space.spaceId !== spaceId) return
    if (error instanceof ApiError && error.status === 401) emit('sessionExpired', error)
    else inviteError.value = error instanceof ApiError ? error.message : '초대를 보내지 못했습니다.'
  }
}

function select() {
  inviteError.value = ''
  inviteSuccess.value = ''
}
</script>

<template>
  <section aria-labelledby="invitation-title">
    <h2 id="invitation-title" class="invitation-heading">사용자 초대</h2>
    <p class="description">로그인 아이디로 함께할 사용자를 찾아 초대하세요.</p>
    <div class="invitation-form user-invitation">
      <div class="field">
        <span class="field-label">초대할 공간</span>
        <div class="fixed-space"><strong>{{ space.title }}</strong><span>현재 선택한 공간</span></div>
      </div>
      <form @submit.prevent="search">
        <label for="search-login-id">로그인 아이디</label>
        <div class="search-controls">
          <input id="search-login-id" v-model="query" type="text" maxlength="20"
            placeholder="로그인 아이디 입력" autocomplete="off" :disabled="spaces.inviting"
            aria-describedby="search-hint" />
          <button class="primary-button" type="submit" :disabled="!validKeyword || searching || spaces.inviting">
            {{ searching ? '검색 중…' : searchError ? '다시 검색' : '검색' }}
          </button>
        </div>
        <p id="search-hint" class="field-hint">2~20자를 입력하세요. 로그인 아이디의 일부로 검색할 수 있습니다.</p>
      </form>
      <p v-if="searching" class="field-hint" role="status">사용자를 찾고 있습니다.</p>
      <p v-if="searchError" class="status-message status-message--error" role="alert">{{ searchError }}</p>
      <fieldset v-if="searched && results.length" class="search-results" :disabled="spaces.inviting">
        <legend>검색 결과 {{ results.length }}명 · 최대 10명</legend>
        <label v-for="user in results" :key="user.userId" class="search-user"
          :class="{ selected: selectedId === user.userId, self: isSelf(user) }">
          <span class="user-avatar" aria-hidden="true">{{ user.name.slice(0, 1) }}</span>
          <span class="user-details"><strong>{{ user.name }}</strong><small>@{{ user.loginId }}</small></span>
          <span v-if="isSelf(user)" class="self-label">본인 · 선택 불가</span>
          <input v-model="selectedId" type="radio" name="invite-target" :value="user.userId"
            :disabled="isSelf(user)" :aria-label="`${user.name} @${user.loginId} 선택`" @change="select" />
        </label>
      </fieldset>
      <p v-else-if="searched" class="field-hint" role="status">일치하는 사용자가 없습니다. 로그인 아이디를 확인하거나 검색어를 바꿔 주세요.</p>
      <div v-if="selectedUser" class="selected-target">초대 대상 <strong>{{ selectedUser.name }}</strong> · @{{ selectedUser.loginId }}</div>
      <button class="primary-button invitation-submit" type="button" :disabled="!canInvite || spaces.inviting" @click="invite">
        {{ spaces.inviting ? '초대 중…' : '초대 보내기' }}
      </button>
      <p v-if="spaces.inviting" class="field-hint" role="status">초대를 보내고 있습니다. 잠시 기다려 주세요.</p>
      <p v-if="inviteError" class="status-message status-message--error" role="alert">{{ inviteError }}</p>
      <p v-if="inviteSuccess" class="status-message status-message--success" role="status">{{ inviteSuccess }}</p>
    </div>
  </section>
</template>

<style scoped>
.invitation-heading { color: #fff3e7; }
.invitation-form.user-invitation { max-width: 740px; display: grid; gap: 22px; margin-top: 20px; border: 1px solid #decfc6; }
.field { display: grid; gap: 8px; }
.field-label, form > label { font-size: 13px; font-weight: 600; }
.fixed-space { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 14px; border: 1px solid #decfc6; border-radius: 9px; }
.fixed-space span, .field-hint, .search-results legend { color: #7b6873; font-size: 12px; line-height: 1.7; }
.fixed-space strong { overflow-wrap: anywhere; }
.fixed-space span { flex-shrink: 0; }
.search-controls { display: flex; gap: 10px; margin-top: 9px; }
.search-controls input { flex: 1; min-width: 0; padding: 13px 14px; font: inherit; border: 1px solid #cdbbbf; }
.search-controls button { flex-shrink: 0; width: auto; min-width: 78px; }
.field-hint { margin: 8px 0 0; }
.search-results { border: 0; margin: 0; padding: 0; min-width: 0; }
.search-results legend { margin-bottom: 10px; }
.search-user { display: flex; gap: 12px; align-items: center; padding: 14px; border: 1px solid #decfc6; border-radius: 10px; margin-bottom: 9px; background: #fffefa; cursor: pointer; }
.search-user.selected { background: #efe5ee; border-color: #8a6b83; box-shadow: inset 0 0 0 1px #8a6b83; }
.search-user:focus-within { outline: 2px solid #8a6b83; outline-offset: 2px; }
.search-user.self { opacity: .65; cursor: default; }
.user-avatar { display: grid; place-items: center; width: 38px; height: 38px; flex-shrink: 0; border-radius: 50%; background: #e1cfdd; color: #71576c; }
.user-details { min-width: 0; flex: 1; overflow-wrap: anywhere; }
.user-details strong { font-size: 14px; }
.user-details small { display: block; margin-top: 4px; color: #7b6873; font-size: 12px; }
.search-user input { width: 18px; height: 18px; margin: 0; accent-color: #71576c; }
.self-label { font-size: 12px; }
.selected-target { padding: 13px 15px; border-radius: 9px; background: #efe5ee; font-size: 14px; overflow-wrap: anywhere; }
.invitation-submit { width: 100%; }
button:disabled { cursor: default; opacity: .5; }
@media (max-width: 500px) { .invitation-form.user-invitation { padding: 18px; } .search-user { gap: 9px; padding: 12px; } .self-label { max-width: 55px; } }
</style>
