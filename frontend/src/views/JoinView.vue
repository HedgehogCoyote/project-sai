<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AuthLayout from '@/components/AuthLayout.vue'
import { ApiError } from '@/services/api'
import { api, relationLabel, type LinkPreview } from '@/services/workspace'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const token = computed(() => String(route.params.token ?? ''))
const preview = ref<LinkPreview | null>(null),
  loading = ref(true),
  joining = ref(false),
  error = ref('')

onMounted(async () => {
  try {
    if (!auth.initialized) await auth.fetchMe().catch(() => undefined)
    preview.value = await api.previewLink(token.value)
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '초대 링크를 확인하지 못했습니다.'
  } finally {
    loading.value = false
  }
})
async function join() {
  joining.value = true
  error.value = ''
  try {
    const res = await api.joinLink(token.value)
    await router.replace({ name: 'home', query: { space: String(res.spaceId) } })
  } catch (e) {
    if (e instanceof ApiError && e.status === 401) {
      auth.clearSession()
      error.value = '로그인이 만료되었습니다. 다시 로그인해 주세요.'
    } else error.value = e instanceof ApiError ? e.message : '공간에 들어가지 못했습니다.'
  } finally {
    joining.value = false
  }
}
const back = computed(() => ({ redirect: route.fullPath }))
</script>

<template>
  <AuthLayout>
    <section class="join" aria-labelledby="join-title">
      <p v-if="loading" role="status">초대 링크를 확인하는 중...</p>
      <template v-else-if="preview">
        <svg class="mark" viewBox="0 0 72 54" aria-hidden="true">
          <circle cx="24" cy="27" r="20" :fill="preview.colorPrimary" />
          <circle cx="48" cy="27" r="20" :fill="preview.colorSecondary" style="mix-blend-mode: multiply" />
        </svg>
        <h1 id="join-title">{{ preview.spaceTitle }}</h1>
        <p class="meta">{{ relationLabel(preview.relation) }} · 멤버 {{ preview.memberCount }}명 · {{ preview.inviterName }}님의 초대</p>
        <p v-if="preview.status !== 'VALID' && !preview.alreadyMember" class="error" role="alert">
          {{ preview.status === 'EXPIRED' ? '기간이 지난 링크입니다. 초대한 사람에게 새 링크를 받아 주세요.' : '더 이상 쓸 수 없는 링크입니다.' }}
        </p>
        <p v-if="error" class="error" role="alert">{{ error }}</p>
        <template v-if="auth.user">
          <button
            v-if="preview.alreadyMember || preview.status === 'VALID'"
            class="primary-button"
            type="button"
            :disabled="joining"
            @click="join"
          >
            {{ preview.alreadyMember ? '공간으로 가기' : joining ? '들어가는 중...' : `${auth.user.name}님으로 들어가기` }}
          </button>
        </template>
        <template v-else-if="preview.status === 'VALID'">
          <p class="meta">계정이 있으면 로그인하고, 없으면 가입하면 바로 이 공간으로 들어옵니다.</p>
          <RouterLink class="primary-button" :to="{ name: 'login', query: back }">로그인하고 들어가기</RouterLink>
          <RouterLink class="secondary" :to="{ name: 'signup', query: back }">처음이라면 가입하기</RouterLink>
        </template>
      </template>
      <template v-else>
        <h1 id="join-title">초대 링크를 열 수 없습니다</h1>
        <p class="error" role="alert">{{ error }}</p>
        <RouterLink class="secondary" to="/">SAI 소개로 가기</RouterLink>
      </template>
    </section>
  </AuthLayout>
</template>

<style scoped>
.join { display: grid; gap: 14px; }
.join h1 { margin: 0; font-size: 26px; letter-spacing: var(--tracking-tight); overflow-wrap: anywhere; }
.mark { width: 64px; height: 48px; }
.meta { margin: 0; color: var(--text-muted); font-size: 14px; line-height: 1.7; }
.error { margin: 0; padding: 11px 14px; border-radius: 4px; background: #fff2f2; color: var(--danger); font-size: 14px; }
.primary-button { display: inline-flex; justify-content: center; align-items: center; text-decoration: none; }
.secondary { justify-self: center; color: var(--navy-900, #71576c); font-size: 14px; }
</style>
