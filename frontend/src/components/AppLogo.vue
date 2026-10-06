<script setup lang="ts">
import logo from '@/assets/brand/logo.svg'
import lightLogo from '@/assets/brand/logo-light.svg'
import mark from '@/assets/brand/logo-mark.svg'
withDefaults(defineProps<{ light?: boolean; compact?: boolean }>(), { light: false, compact: false })
</script>

<template>
  <div class="logo" :class="{ 'logo--light': light, 'logo--compact': compact }" aria-label="SAI">
    <img
      class="logo__image"
      :src="compact ? mark : light ? lightLogo : logo"
      alt=""
      aria-hidden="true"
      :width="compact ? 40 : 117"
      :height="compact ? 36 : 40"
    />
  </div>
</template>

<style scoped>
.logo { display: inline-flex; align-items: center; flex: 0 0 auto; }
.logo__image { display: block; width: 117px; height: 40px; object-fit: contain; }
.logo--compact .logo__image { width: 40px; height: 36px; }
.logo--compact.logo--light .logo__image { filter: brightness(0) invert(1); }
</style>
