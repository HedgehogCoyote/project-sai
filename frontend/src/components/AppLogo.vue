<script setup lang="ts">
withDefaults(defineProps<{ light?: boolean; compact?: boolean }>(), { light: false, compact: false })
</script>

<template>
  <div class="logo" :class="{ 'logo--light': light, 'logo--compact': compact }" aria-label="SAI">
    <span class="logo__mark" aria-hidden="true">
      <span class="logo__circle logo__circle--left"></span>
      <span class="logo__circle logo__circle--right"></span>
    </span>
    <span v-if="!compact" class="logo__word">sai</span>
  </div>
</template>

<style scoped>
.logo { display: inline-flex; align-items: center; gap: .65rem; color: var(--navy-900); }
.logo--light { color: #fff; }
.logo__mark { position: relative; display: block; width: 3.3rem; height: 2rem; flex: 0 0 auto; }
.logo__circle { position: absolute; top: 0; width: 2rem; height: 2rem; border-radius: 50%; }
.logo__circle--left { left: 0; background: linear-gradient(135deg,#7776f4,#a7a3ff); }
.logo__circle--right { right: 0; background: linear-gradient(135deg,#7757d7,#b17ce8); opacity: .92; }
.logo__circle--left::after { position: absolute; inset: 0 0 0 35%; border-radius: 50%; background: #5145b6; content: ''; clip-path: circle(50% at 100% 50%); opacity: .72; }
.logo__word { font-size: 2rem; font-weight: 750; line-height: 1; letter-spacing: -.08em; }
.logo--compact .logo__mark { width: 2.8rem; height: 1.7rem; }
.logo--compact .logo__circle { width: 1.7rem; height: 1.7rem; }
</style>
