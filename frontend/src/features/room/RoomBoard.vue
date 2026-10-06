<script setup lang="ts">
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import RoomCanvas from './RoomCanvas.vue'
import { MODULES, type Activity, type ModuleId } from '@/features/content/types'
import {
  createFurniture,
  firstFree,
  fits,
  seedRoom,
  validRoom,
  furnitureNames,
  type Furniture,
  type FurnitureKind,
} from './model'
const props = defineProps<{
  owner: string
  spaceId: number
  modules: ModuleId[]
  activities: Activity[]
  blocked: boolean
}>()
const emit = defineEmits<{
  open: [view: string, event: string | null]
  invite: []
  createEvent: []
  modules: []
}>()
const items = ref<Furniture[]>([]),
  draft = ref<Furniture[]>([]),
  editing = ref(false),
  selected = ref<string | null>(null),
  gridVisible = ref(true),
  time = ref<'day' | 'evening'>('evening'),
  message = ref(''),
  previews = ref<Record<string, string>>({}),
  loadFailed = ref(false)
const key = `sai:room:v1:${encodeURIComponent(props.owner)}:${props.spaceId}`,
  timeKey = `sai:room-time:${encodeURIComponent(props.owner)}`
const visibleItems = computed(() => (editing.value ? draft.value : items.value)),
  object = computed(() => draft.value.find((o) => o.id === selected.value)),
  events = computed(() =>
    props.activities.filter((a) => !a.archived && a.status === 'ongoing').slice(0, 2),
  ),
  dirty = computed(
    () => editing.value && JSON.stringify(items.value) !== JSON.stringify(draft.value),
  )
const tools = computed(() => MODULES.filter((m) => props.modules.includes(m.id)))
const connections = computed(() => {
  const target = props.activities.find((a) => a.id === object.value?.event)
  return MODULES.filter((m) => (target?.modules ?? props.modules).includes(m.id))
})
onMounted(() => {
  items.value = seedRoom(
    props.modules,
    props.activities.find((a) => a.modules.includes('expenses'))?.id,
  )
  try {
    const stored = localStorage.getItem(key)
    if (stored) {
      const parsed: unknown = JSON.parse(stored)
      if (!validRoom(parsed)) throw new Error()
      items.value = parsed
    }
    time.value = localStorage.getItem(timeKey) === 'day' ? 'day' : 'evening'
  } catch {
    loadFailed.value = true
    message.value = '저장된 방을 읽지 못했어요. 기존 저장 데이터는 변경하지 않았습니다.'
  }
  window.addEventListener('beforeunload', beforeUnload)
})
onBeforeUnmount(() => window.removeEventListener('beforeunload', beforeUnload))
function beforeUnload(e: BeforeUnloadEvent) {
  if (dirty.value) {
    e.preventDefault()
    e.returnValue = ''
  }
}
function canLeave() {
  return (
    !dirty.value || window.confirm('저장하지 않은 가구 배치가 있습니다. 저장하지 않고 나갈까요?')
  )
}
defineExpose({ canLeave, dirty })
function start() {
  draft.value = JSON.parse(JSON.stringify(items.value))
  selected.value = draft.value[0]?.id ?? null
  editing.value = true
  message.value = ''
}
function cancel() {
  editing.value = false
  draft.value = []
  selected.value = null
  message.value = ''
}
function save() {
  if (!validRoom(draft.value)) {
    message.value = '가구 배치를 확인해주세요.'
    return
  }
  try {
    localStorage.setItem(key, JSON.stringify(draft.value))
    items.value = JSON.parse(JSON.stringify(draft.value))
    editing.value = false
    loadFailed.value = false
    message.value = '가구 배치를 저장했어요.'
  } catch {
    message.value = '저장하지 못했어요. 배치는 유지되어 있으니 다시 저장해주세요.'
  }
}
function move(id: string, x: number, y: number) {
  if (props.blocked) return
  const o = draft.value.find((o) => o.id === id)
  if (!o) return
  if (!fits(draft.value, o, x, y)) {
    message.value = '방 밖이거나 다른 가구가 놓인 칸이에요.'
    return
  }
  o.x = x
  o.y = y
  message.value = ''
}
function nudge(dx: number, dy: number) {
  if (object.value) move(object.value.id, object.value.x + dx, object.value.y + dy)
}
function add(kind: FurnitureKind) {
  const o = createFurniture(kind),
    p = firstFree(draft.value, o)
  if (!p) {
    message.value = '빈 칸이 부족해요. 가구를 먼저 치워주세요.'
    return
  }
  Object.assign(o, p)
  draft.value.push(o)
  selected.value = o.id
}
function rotate() {
  const o = object.value
  if (!o) return
  const next = { ...o, w: o.h, h: o.w }
  if (!fits(draft.value, next, o.x, o.y)) {
    message.value = '회전할 공간이 부족해요.'
    return
  }
  ;[o.w, o.h] = [o.h, o.w]
  o.rotation = (o.rotation + 1) % 4
}
function remove() {
  draft.value = draft.value.filter((o) => o.id !== selected.value)
  selected.value = draft.value[0]?.id ?? null
}
function setTime(value: 'day' | 'evening') {
  time.value = value
  try {
    localStorage.setItem(timeKey, value)
  } catch {
    message.value = '분위기 설정을 저장하지 못했어요.'
  }
}
function binding() {
  if (object.value) object.value.label = ''
}
function target() {
  if (
    object.value &&
    !connections.value.some((m) => m.id === object.value?.module) &&
    object.value.module !== 'activities'
  ) {
    object.value.module = ''
    object.value.label = ''
  }
}
function open(id: string) {
  if (props.blocked) return
  const o = items.value.find((o) => o.id === id)
  if (!o?.module) {
    message.value = '장식 가구예요. 방 꾸미기에서 기능을 연결할 수 있어요.'
    return
  }
  if (o.module === 'activities') {
    emit('open', 'activities', null)
    return
  }
  const event = props.activities.find((a) => a.id === o.event)
  if (o.event && !event) {
    message.value = '연결된 이벤트를 찾을 수 없어요.'
    return
  }
  if (!(event?.modules ?? props.modules).includes(o.module as ModuleId)) {
    message.value = '해당 도구가 꺼져 있어요. 도구 선택에서 추가해주세요.'
    return
  }
  emit('open', o.module, event?.id ?? null)
}
</script>
<template>
  <div class="room-board" :data-time="time">
    <div class="room-toolbar">
      <p>
        {{
          editing
            ? '가구 선택 → 위치 배치 → 기능 연결'
            : '가구로 도구를 열거나, 오른쪽 메뉴에서 바로 사용하세요.'
        }}
      </p>
      <div>
        <template v-if="editing"
          ><button :disabled="blocked" @click="cancel">변경 취소</button
          ><button class="room-primary" :disabled="blocked" @click="save">
            배치 저장
          </button></template
        ><template v-else
          ><button :disabled="blocked" @click="emit('invite')">멤버 초대</button
          ><button class="room-primary" :disabled="blocked" @click="start">
            방 꾸미기
          </button></template
        >
      </div>
    </div>
    <p v-if="message" class="room-message" role="status">{{ message }}</p>
    <div class="room-workspace" :class="{ editing }">
      <aside v-if="editing" class="room-panel inventory">
        <h2>가구 추가</h2>
        <div class="furniture-list">
          <button
            v-for="(name, kind) in furnitureNames"
            :key="kind"
            :disabled="blocked"
            @click="add(kind)"
          >
            <img v-if="previews[kind]" :src="previews[kind]" alt="" /><span>{{ name }}</span>
          </button>
        </div>
      </aside>
      <section class="room-stage">
        <RoomCanvas
          :items="visibleItems"
          :editing="editing && !blocked"
          :selected="selected"
          :time="time"
          :grid-visible="gridVisible"
          @select="selected = $event"
          @move="move"
          @open="open"
          @error="message = $event"
          @previews="previews = $event"
        />
        <div class="room-atmosphere" role="group" aria-label="방 분위기">
          <button :aria-pressed="time === 'day'" @click="setTime('day')">☀ 낮</button
          ><button :aria-pressed="time === 'evening'" @click="setTime('evening')">☾ 저녁</button>
        </div>
        <label v-if="editing" class="room-grid-toggle"
          ><input v-model="gridVisible" type="checkbox" /> 격자 표시
          <span>초록색은 배치 가능, 붉은색은 겹치는 칸이에요.</span></label
        >
      </section>
      <aside class="room-panel">
        <template v-if="editing"
          ><template v-if="object"
            ><h2>{{ furnitureNames[object.kind] }}</h2>
            <img
              v-if="previews[object.kind]"
              class="selected-preview"
              :src="previews[object.kind]"
              alt="선택한 가구"
            />
            <p class="room-muted">차지하는 격자 · {{ object.w }} × {{ object.h }}칸</p>
            <label
              >연결 대상<select v-model="object.event" :disabled="blocked" @change="target">
                <option :value="undefined">공간 전체</option>
                <option v-for="event in activities" :key="event.id" :value="event.id">
                  {{ event.title }}
                </option>
              </select></label
            ><label
              >연결할 기능<select v-model="object.module" :disabled="blocked" @change="binding">
                <option value="">장식으로만 사용</option>
                <option value="activities">이벤트 목록</option>
                <option v-for="m in connections" :key="m.id" :value="m.id">{{ m.label }}</option>
              </select></label
            ><label
              >표시 이름<input
                v-model="object.label"
                maxlength="24"
                :disabled="blocked"
                placeholder="예: 여행 정산"
            /></label>
            <p class="room-muted">현재 위치: {{ object.x + 1 }}, {{ object.y + 1 }}</p>
            <div class="room-keypad">
              <button aria-label="가구 위로 이동" :disabled="blocked" @click="nudge(0, -1)">
                ↑</button
              ><button aria-label="가구 왼쪽으로 이동" :disabled="blocked" @click="nudge(-1, 0)">
                ←</button
              ><button aria-label="가구 오른쪽으로 이동" :disabled="blocked" @click="nudge(1, 0)">
                →</button
              ><button aria-label="가구 아래로 이동" :disabled="blocked" @click="nudge(0, 1)">
                ↓
              </button>
            </div>
            <button :disabled="blocked" @click="rotate">↻ 90° 회전</button
            ><button :disabled="blocked" @click="remove">가구 치우기</button>
            <p class="room-muted">연결한 도구의 데이터는 유지돼요.</p></template
          >
          <p v-else>가구를 선택하거나 추가하세요.</p></template
        ><template v-else
          ><h2>예정된 이벤트</h2>
          <article v-for="event in events" :key="event.id" class="room-event">
            <h3>{{ event.title }}</h3>
            <p>{{ event.start || '날짜 미정' }}{{ event.end ? ' — ' + event.end : '' }}</p>
            <button
              class="room-primary"
              :disabled="blocked"
              @click="emit('open', 'home', event.id)"
            >
              이벤트 열기 →
            </button>
          </article>
          <p v-if="!events.length" class="room-muted">예정된 이벤트가 없어요.</p>
          <button :disabled="blocked" @click="emit('createEvent')">＋ 이벤트 만들기</button>
          <hr />
          <h2>공간의 도구</h2>
          <div class="room-tools">
            <button
              v-for="m in tools"
              :key="m.id"
              :disabled="blocked"
              @click="emit('open', m.id, null)"
            >
              {{ m.label }} →
            </button>
          </div>
          <button :disabled="blocked" @click="emit('modules')">도구 선택</button></template
        >
      </aside>
    </div>
    <p class="room-storage-note">방 꾸미기는 이 계정·공간별로 현재 브라우저에 저장됩니다.</p>
  </div>
</template>
<style scoped>
.room-board {
  color: #f7ede6;
}
.room-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 15px;
  margin-bottom: 16px;
}
.room-toolbar > p {
  margin: 0;
  font-size: 13px;
}
.room-toolbar > div {
  display: flex;
  gap: 8px;
}
.room-board button {
  font: inherit;
  font-size: 13px;
  border: 1px solid #d6c8bd;
  border-radius: 9px;
  background: #fff7eb;
  color: #5d4b57;
  padding: 10px 13px;
  cursor: pointer;
}
.room-board button:hover {
  background: #eee0ce;
}
.room-board button.room-primary {
  background: #71576c;
  border-color: #71576c;
  color: #fff7ec;
}
.room-workspace {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 270px;
  gap: 24px;
  align-items: start;
}
.room-workspace.editing {
  grid-template-columns: 140px minmax(0, 1fr) 240px;
  gap: 18px;
}
.room-panel {
  background: #fff7ebed;
  border: 1px solid #fff4dfb0;
  border-radius: 14px;
  padding: 20px;
  color: #4b4048;
  box-shadow: 0 8px 24px #54405412;
}
.room-panel h2 {
  font-size: 16px;
  margin: 0 0 15px;
}
.room-panel h3 {
  font-size: 19px;
  margin: 0 0 7px;
  color: #705969;
}
.room-event {
  margin: 20px 0;
}
.room-event p,
.room-muted {
  font-size: 12px;
  color: #75666c;
  line-height: 1.7;
}
.room-event button {
  width: 100%;
}
.room-tools {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 7px;
  margin: 12px 0;
}
.room-tools button {
  text-align: left;
  background: #f2e7da;
  font-size: 12px;
  padding: 11px 8px;
}
.room-panel hr {
  border: 0;
  border-top: 1px solid #dfcfc0;
  margin: 24px 0;
}
.room-panel label {
  display: grid;
  gap: 7px;
  font-size: 12px;
  margin-top: 14px;
}
.room-panel input,
.room-panel select {
  width: 100%;
  padding: 9px 10px;
  font: inherit;
  border: 1px solid #dacbbb;
  border-radius: 7px;
  background: #fffaf2;
  color: #4b4048;
}
.room-panel > .room-muted {
  margin: 12px 0;
}
.selected-preview {
  display: block;
  width: 120px;
  height: 95px;
  object-fit: contain;
  margin: auto;
}
.furniture-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 7px;
}
.furniture-list button {
  padding: 5px 2px;
  font-size: 11px;
  background: #f6ecdd;
}
.furniture-list img {
  width: 100%;
  height: 55px;
  object-fit: contain;
  display: block;
}
.room-atmosphere {
  display: flex;
  width: fit-content;
  gap: 4px;
  margin: 0 auto 15px;
  padding: 4px;
  background: #67516440;
  border: 1px solid #eddfd54d;
  border-radius: 12px;
}
.room-atmosphere button {
  background: transparent;
  border-color: transparent;
  color: #fff3e8;
  padding: 7px 12px;
}
.room-atmosphere button[aria-pressed='true'] {
  background: #f7e7d5;
  color: #644e60;
}
.room-grid-toggle {
  font-size: 12px;
  display: flex;
  gap: 8px;
  align-items: center;
}
.room-grid-toggle span {
  margin-left: auto;
  font-size: 11px;
}
.room-keypad {
  display: grid;
  grid-template-columns: repeat(3, 36px);
  gap: 4px;
  justify-content: center;
  margin: 15px 0;
}
.room-keypad button:first-child {
  grid-column: 2;
}
.room-keypad button:nth-child(2) {
  grid-column: 1;
}
.room-keypad button:nth-child(4) {
  grid-column: 2;
}
.room-keypad button {
  padding: 7px;
}
.room-panel > button {
  margin: 5px 3px 0 0;
}
.room-message {
  background: #fff3e5ee;
  color: #665164;
  padding: 11px 15px;
  border-radius: 9px;
  font-size: 13px;
}
.room-storage-note {
  font-size: 11px;
  opacity: 0.85;
  margin-top: 22px;
}
.room-board[data-time='day'] .room-stage {
  background: radial-gradient(ellipse, #bbcab580, transparent 70%);
  border-radius: 20px;
}
@media (max-width: 1100px) {
  .room-workspace.editing {
    grid-template-columns: 120px minmax(0, 1fr) 220px;
    gap: 12px;
  }
}
@media (max-width: 900px) {
  .room-workspace.editing {
    grid-template-columns: minmax(0, 1fr) 230px;
  }
  .inventory {
    grid-column: 1/-1;
  }
  .furniture-list {
    display: flex;
    flex-wrap: wrap;
  }
  .furniture-list button {
    width: 70px;
  }
  .room-workspace {
    gap: 14px;
    grid-template-columns: minmax(0, 1fr) 240px;
  }
}
@media (max-width: 650px) {
  .room-workspace,
  .room-workspace.editing {
    grid-template-columns: 1fr;
  }
  .room-toolbar {
    flex-wrap: wrap;
  }
  .inventory {
    grid-column: auto;
  }
  .room-grid-toggle {
    flex-wrap: wrap;
  }
  .room-grid-toggle span {
    margin-left: 0;
  }
}
</style>
