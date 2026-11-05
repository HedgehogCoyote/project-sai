<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ApiError } from '@/services/api'
import { api } from '@/services/workspace'
import { useAuthStore } from '@/stores/auth'
import { useSpacesStore } from '@/stores/spaces'

const auth = useAuthStore()
const spaces = useSpacesStore()
const router = useRouter()
const name = ref(auth.user?.name ?? ''),
  current = ref(''),
  next = ref(''),
  confirmNext = ref(''),
  deletePassword = ref(''),
  deleting = ref(false),
  busy = ref(false),
  error = ref(''),
  ok = ref('')

async function run(action: () => Promise<unknown>, done: string, fallback: string) {
  busy.value = true
  error.value = ''
  ok.value = ''
  try {
    await action()
    ok.value = done
    return true
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : fallback
    return false
  } finally {
    busy.value = false
  }
}
async function saveName() {
  if (await run(() => api.updateProfile(name.value.trim()), '이름을 바꿨습니다.', '이름을 바꾸지 못했습니다.'))
    await auth.fetchMe()
}
async function savePassword() {
  if (next.value !== confirmNext.value) {
    error.value = '새 비밀번호가 서로 다릅니다.'
    return
  }
  if (await run(() => api.changePassword(current.value, next.value), '비밀번호를 바꿨습니다.', '비밀번호를 바꾸지 못했습니다.'))
    current.value = next.value = confirmNext.value = ''
}
async function remove() {
  if (await run(() => api.deleteAccount(deletePassword.value), '탈퇴했습니다.', '탈퇴하지 못했습니다.')) {
    auth.clearSession()
    spaces.clearSpaces()
    await router.replace('/')
  }
}
</script>

<template>
  <section class="ws-section account-settings" aria-label="내 계정">
    <h1>내 계정</h1>
    <p class="ws-hint">@{{ auth.user?.loginId }} · {{ auth.user?.email }}</p>
    <p v-if="error" class="ws-error" role="alert">{{ error }}</p>
    <p v-if="ok" class="ws-ok" role="status">{{ ok }}</p>
    <form class="ws-form ws-card" @submit.prevent="saveName">
      <label>이름<input v-model="name" maxlength="50" required :disabled="busy" /></label>
      <p class="ws-hint">공간마다 다른 이름을 쓰려면 각 공간의 사람 메뉴에서 바꿀 수 있습니다.</p>
      <div class="ws-actions"><button class="ws-btn primary" type="submit" :disabled="busy || !name.trim()">이름 저장</button></div>
    </form>
    <form class="ws-form ws-card" @submit.prevent="savePassword">
      <strong>비밀번호 바꾸기</strong>
      <label>현재 비밀번호<input v-model="current" type="password" autocomplete="current-password" required :disabled="busy" /></label>
      <label>새 비밀번호 (8~30자)<input v-model="next" type="password" autocomplete="new-password" minlength="8" maxlength="30" required :disabled="busy" /></label>
      <label>새 비밀번호 확인<input v-model="confirmNext" type="password" autocomplete="new-password" required :disabled="busy" /></label>
      <div class="ws-actions"><button class="ws-btn primary" type="submit" :disabled="busy">비밀번호 바꾸기</button></div>
    </form>
    <p class="ws-hint">비밀번호를 잊었다면 아직은 이메일 재설정이 없습니다. 공간 소유자에게 새 계정으로 다시 초대받아 주세요.</p>
    <div class="ws-danger">
      <strong>탈퇴하기</strong>
      <p class="ws-hint">혼자 있는 공간은 삭제되고, 다른 멤버가 있는 공간에서는 빠집니다. 소유자인 공간이 있다면 먼저 소유권을 넘겨 주세요. 남긴 댓글과 사진은 "탈퇴한 사용자"로 남습니다.</p>
      <button v-if="!deleting" class="ws-btn danger" type="button" @click="deleting = true">탈퇴 진행</button>
      <form v-else class="ws-form" @submit.prevent="remove">
        <label>확인을 위해 비밀번호를 입력해 주세요<input v-model="deletePassword" type="password" autocomplete="current-password" required :disabled="busy" /></label>
        <div class="ws-actions">
          <button class="ws-btn danger" type="submit" :disabled="busy || !deletePassword">탈퇴하기</button>
          <button class="ws-btn" type="button" :disabled="busy" @click="deleting = false">취소</button>
        </div>
      </form>
    </div>
  </section>
</template>

<style scoped>
.account-settings { max-width: 560px; }
.account-settings h1 { margin: 0; font-size: 26px; }
</style>
