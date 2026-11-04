<script setup lang="ts">
import { ref } from 'vue'
import { api, EMOJIS, type Post } from '@/services/workspace'

const props = defineProps<{ spaceId: number; post: Post }>()
const emit = defineEmits<{ updated: [post: Post] }>()
const busy = ref(false)
async function toggle(emoji: string) {
  if (busy.value) return
  busy.value = true
  try {
    emit('updated', await api.react(props.spaceId, props.post.id, emoji))
  } catch {
    /* a failed reaction leaves the counts as they were */
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="reactions" role="group" aria-label="반응">
    <button
      v-for="e in EMOJIS"
      :key="e"
      type="button"
      :class="{ on: post.myReactions.includes(e), used: !!post.reactions[e] }"
      :aria-pressed="post.myReactions.includes(e)"
      :aria-label="`${e} 반응 ${post.reactions[e] ?? 0}개`"
      :disabled="busy"
      @click="toggle(e)"
    >
      {{ e }}<span v-if="post.reactions[e]">{{ post.reactions[e] }}</span>
    </button>
  </div>
</template>

<style scoped>
.reactions { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 4px; }
.reactions button { display: inline-flex; align-items: center; gap: 3px; min-height: 26px; padding: 0 7px; border: 1px solid transparent; border-radius: 999px; background: none; font-size: 13px; cursor: pointer; opacity: .45; }
.reactions button.used, .reactions button:hover { opacity: 1; border-color: #e3d3cc; background: #fff; }
.reactions button.on { opacity: 1; border-color: #71576c; background: #f3e9ef; }
.reactions span { font-size: 11px; color: #5d4b57; font-variant-numeric: tabular-nums; }
</style>
