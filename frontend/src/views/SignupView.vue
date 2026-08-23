<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AuthLayout from '@/components/AuthLayout.vue'
import { ApiError } from '@/services/api'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore(); const router = useRouter(); const showPassword = ref(false); const errorMessage = ref('')
const form = reactive({ name:'', loginId:'', email:'', phoneNumber:'', password:'' })

function formatPhoneNumber(){
  const value=form.phoneNumber.replace(/\D/g,'').slice(0,11)
  form.phoneNumber=value.length<=3?value:value.length<=7?`${value.slice(0,3)}-${value.slice(3)}`:`${value.slice(0,3)}-${value.slice(3,7)}-${value.slice(7)}`
}
async function submit(){
  errorMessage.value=''
  try { await auth.signup({...form,loginId:form.loginId.trim(),email:form.email.trim()}); await router.replace('/') }
  catch(error){ errorMessage.value=error instanceof ApiError?error.message:'회원가입 중 문제가 발생했습니다.' }
}
</script>

<template>
  <AuthLayout>
    <div class="form-heading form-heading--compact"><p class="eyebrow">CREATE YOUR SPACE</p><h2>우리의 사이를 시작해요</h2><p>간단한 정보만 입력하면 바로 공간을 만들 수 있어요.</p></div>
    <form class="auth-form auth-form--signup" @submit.prevent="submit">
      <div class="form-grid">
        <div class="field"><label for="name">이름</label><input id="name" v-model="form.name" autocomplete="name" placeholder="이름" required></div>
        <div class="field"><label for="loginId">아이디</label><input id="loginId" v-model="form.loginId" autocomplete="username" minlength="4" maxlength="25" placeholder="4자 이상" required></div>
      </div>
      <div class="field"><label for="email">이메일</label><input id="email" v-model="form.email" type="email" autocomplete="email" placeholder="name@example.com" required></div>
      <div class="field"><label for="phoneNumber">휴대폰 번호</label><input id="phoneNumber" v-model="form.phoneNumber" type="tel" autocomplete="tel" placeholder="010-0000-0000" required @input="formatPhoneNumber"></div>
      <div class="field"><label for="password">비밀번호</label><div class="field__password"><input id="password" v-model="form.password" :type="showPassword?'text':'password'" autocomplete="new-password" minlength="8" maxlength="30" placeholder="8자 이상 입력하세요" required><button type="button" :aria-label="showPassword?'비밀번호 숨기기':'비밀번호 보기'" @click="showPassword=!showPassword">{{ showPassword?'숨김':'보기' }}</button></div></div>
      <p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>
      <button class="primary-button" type="submit" :disabled="auth.pending"><span v-if="auth.pending" class="spinner"></span>{{ auth.pending?'계정 만드는 중...':'SAI 시작하기' }}</button>
    </form>
    <p class="form-switch">이미 계정이 있나요? <RouterLink to="/login">로그인</RouterLink></p>
  </AuthLayout>
</template>
