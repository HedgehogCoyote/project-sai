<script setup lang="ts">
import AppLogo from '@/components/AppLogo.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
</script>

<template>
  <div v-if="!auth.initialized" class="app-loading" aria-label="SAI 불러오는 중">
    <AppLogo />
    <span></span>
  </div>
  <RouterView v-else />
</template>

<style scoped>
.app-loading { min-height: 100svh; display: grid; place-content: center; justify-items: center; gap: 1.5rem; background: #fbfaff; }
.app-loading>span { width: 2rem; height: 2px; overflow: hidden; position: relative; background: #e1def4; }
.app-loading>span::after { position: absolute; inset: 0; background: #695fd6; content: ''; transform: translateX(-100%); animation: loading 1s ease-in-out infinite; }
@keyframes loading { 50%,100% { transform: translateX(100%); } }
</style>
