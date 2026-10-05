<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AuthLayout from '@/components/AuthLayout.vue'
import { ApiError } from '@/services/api'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const loginId = ref('')
const password = ref('')
const showPassword = ref(false)
const errorMessage = ref(
  route.query.status === 'server-unavailable'
    ? '서버에 연결할 수 없습니다. 실행 상태를 확인해 주세요.'
    : '',
)

async function submit() {
  errorMessage.value = ''
  try {
    await auth.login(loginId.value.trim(), password.value)
    await router.replace(typeof route.query.redirect === 'string' ? route.query.redirect : '/')
  } catch (error) {
    errorMessage.value =
      error instanceof ApiError ? error.message : '로그인 중 문제가 발생했습니다.'
  }
}
</script>

<template>
  <AuthLayout>
    <div class="form-heading">
      <h2>로그인</h2>
      <p>계정으로 로그인해 내 공간을 확인하세요.</p>
    </div>
    <form class="auth-form" @submit.prevent="submit">
      <div class="field">
        <label for="loginId">아이디</label
        ><input
          id="loginId"
          v-model="loginId"
          autocomplete="username"
          minlength="4"
          maxlength="25"
          placeholder="아이디를 입력하세요"
          required
        />
      </div>
      <div class="field">
        <label for="password">비밀번호</label>
        <div class="field__password">
          <input
            id="password"
            v-model="password"
            :type="showPassword ? 'text' : 'password'"
            autocomplete="current-password"
            minlength="8"
            maxlength="30"
            placeholder="비밀번호를 입력하세요"
            required
          /><button
            type="button"
            :aria-label="showPassword ? '비밀번호 숨기기' : '비밀번호 보기'"
            @click="showPassword = !showPassword"
          >
            {{ showPassword ? '숨김' : '보기' }}
          </button>
        </div>
      </div>
      <p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>
      <button class="primary-button" type="submit" :disabled="auth.pending">
        <span v-if="auth.pending" class="spinner"></span
        >{{ auth.pending ? '로그인 중...' : '로그인' }}
      </button>
    </form>
    <p class="form-switch">
      아직 SAI 계정이 없나요? <RouterLink to="/signup">회원가입</RouterLink>
    </p>
    <p class="auth-footer">© 2026 SAI</p>
  </AuthLayout>
</template>
