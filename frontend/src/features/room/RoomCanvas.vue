<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'
import type { Furniture } from './model'
const props = defineProps<{
  items: Furniture[]
  editing: boolean
  selected: string | null
  time: 'day' | 'evening'
  gridVisible: boolean
}>()
const emit = defineEmits<{
  select: [id: string]
  move: [id: string, x: number, y: number]
  open: [id: string]
  error: [message: string]
  previews: [urls: Record<string, string>]
}>()
const element = ref<HTMLElement | null>(null),
  error = ref('')
let alive = true,
  engine: { update: () => Promise<void>; dispose: () => void } | undefined
onMounted(async () => {
  try {
    const { createRoomRenderer } = await import('./engine.js')
    if (!alive || !element.value) return
    engine = createRoomRenderer(element.value, () => ({ ...props }), {
      select: (id: string) => emit('select', id),
      move: (id: string, x: number, y: number) => emit('move', id, x, y),
      open: (id: string) => emit('open', id),
      error: (m: string) => emit('error', m),
      previews: (urls: Record<string, string>) => emit('previews', urls),
    })
    await engine?.update()
  } catch {
    error.value =
      '3D 화면을 불러오지 못했어요. 새로고침하거나 브라우저의 하드웨어 가속을 확인해주세요.'
  }
})
watch(
  () => [props.items, props.editing, props.selected, props.time, props.gridVisible],
  () => {
    engine?.update().catch(() => {
      error.value = '방을 표시하지 못했어요. 다시 열어주세요.'
    })
  },
  { deep: true },
)
onBeforeUnmount(() => {
  alive = false
  engine?.dispose()
})
</script>
<template>
  <div class="room-canvas-wrap">
    <p v-if="error" role="alert">{{ error }}</p>
    <div v-else ref="element" class="three-room" role="group" aria-label="3D 공유 방"></div>
  </div>
</template>
<style>
.three-room {
  position: relative;
  width: 100%;
  height: 490px;
  touch-action: none;
  isolation: isolate;
}
.three-room canvas {
  display: block;
  width: 100%;
  height: 100%;
}
.three-object-label {
  position: absolute;
  z-index: 2;
  transform: translate(-50%, -100%);
  padding: 5px 9px;
  font-size: 11px;
  white-space: nowrap;
  background: #fff3e5ef;
  border: 1px solid #cfb6bd;
  border-radius: 8px;
  box-shadow: 0 2px 5px #62495715;
  touch-action: none;
  cursor: pointer;
  color: #5b4358;
}
.three-object-label.decorative {
  padding: 3px 7px;
  background: #efe4df99;
  color: #4c3c4c;
  border-color: #e7d5d344;
  font-size: 10px;
}
.three-object-label:hover {
  background: #ffe4be;
}
.three-error {
  padding: 90px 25px;
  text-align: center;
}
.room-canvas-wrap > p {
  padding: 40px;
  text-align: center;
}
.three-room:empty::after {
  content: '가구를 불러오고 있어요…';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 13px;
}
@media (max-width: 1000px) {
  .three-room {
    height: 420px;
  }
}
@media (max-width: 650px) {
  .three-room {
    height: 370px;
  }
}
</style>
