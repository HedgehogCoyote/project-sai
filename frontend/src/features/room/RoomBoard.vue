<script setup lang="ts">
import { computed, ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { ApiError, apiRequest } from '@/services/api'
import type { SpaceRole } from '@/stores/spaces'
import { RoomLocalData, fromLayout, readResponse, toLayout, type RoomResponse } from './server'
import RoomCanvas from './RoomCanvas.vue'
import { MODULES, type Activity, type ModuleId } from '@/features/content/types'
import {
  createFurniture,
  firstFree,
  fits,
  validRoom,
  furnitureNames,
  type Furniture,
  type FurnitureKind,
} from './model'
const props = defineProps<{
  owner: string
  spaceId: number
  role: SpaceRole
  modules: ModuleId[]
  activities: Activity[]
  blocked: boolean
}>()
const emit = defineEmits<{
  open: [view: string, event: string | null]
  invite: []
  createEvent: []
  modules: []
  sessionExpired: [error: ApiError]
}>()
const items = ref<Furniture[]>([]),
  draft = ref<Furniture[]>([]),
  editing = ref(false),
  selected = ref<string | null>(null),
  gridVisible = ref(true),
  time = ref<'day' | 'evening'>('evening'),
  message = ref(''),
  previews = ref<Record<string, string>>({}),
  loadFailed = ref(false),
  loading = ref(true),
  saving = ref(false),
  conflict = ref(false),
  previewing = ref(false),
  latest = ref<RoomResponse | null>(null),
  version = ref(0),
  updatedAt = ref<string | null>(null),
  hasOriginal = ref(false),
  hideImports = ref(false),
  hasRecovery = ref(false),
  localFailure = ref(false)
const timeKey = `sai:room-time:${encodeURIComponent(props.owner)}`
let local = new RoomLocalData(props.owner, props.spaceId, localStorage)
let alive = true,
  requestId = 0,
  controller: AbortController | undefined
const canEdit = computed(() => props.role === 'OWNER' || props.role === 'MANAGER')
const locked = computed(
  () =>
    props.blocked ||
    loading.value ||
    saving.value ||
    !canEdit.value ||
    previewing.value ||
    loadFailed.value ||
    localFailure.value,
)
const editLocked = computed(() => locked.value || conflict.value)
const latestItems = computed(() =>
  latest.value ? fromLayout(latest.value.layout, local.bindings()) : [],
)
const visibleItems = computed(() =>
    previewing.value ? latestItems.value : editing.value ? draft.value : items.value,
  ),
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
  try {
    time.value = localStorage.getItem(timeKey) === 'day' ? 'day' : 'evening'
  } catch {
    message.value = '분위기 설정을 읽지 못했어요.'
  }
  void load()
  window.addEventListener('beforeunload', beforeUnload)
})
onBeforeUnmount(() => {
  alive = false
  requestId++
  controller?.abort()
  window.removeEventListener('beforeunload', beforeUnload)
})
watch(
  () => [props.owner, props.spaceId],
  () => {
    requestId++
    controller?.abort()
    local = new RoomLocalData(props.owner, props.spaceId, localStorage)
    items.value = []
    draft.value = []
    editing.value = false
    selected.value = null
    version.value = 0
    latest.value = null
    previewing.value = false
    conflict.value = false
    saving.value = false
    message.value = ''
    void load()
  },
  { flush: 'sync' },
)
function failure(error: unknown, fallback: string) {
  if (error instanceof ApiError && error.status === 401) emit('sessionExpired', error)
  message.value =
    error instanceof ApiError && error.status === 403
      ? '이 공간의 배치를 변경하거나 조회할 권한이 없어요. 편집 초안은 유지됩니다.'
      : error instanceof Error && !(error instanceof ApiError)
        ? error.message
        : fallback
}
async function load() {
  const current = ++requestId,
    spaceId = props.spaceId
  controller?.abort()
  controller = new AbortController()
  loading.value = true
  loadFailed.value = false
  localFailure.value = false
  hasOriginal.value = false
  hideImports.value = false
  hasRecovery.value = false
  try {
    const response = readResponse(
      await apiRequest(`/api/spaces/${spaceId}/room`, { signal: controller.signal }),
      spaceId,
    )
    if (!alive || current !== requestId) return
    items.value = fromLayout(response.layout)
    version.value = response.version
    updatedAt.value = response.updatedAt
    try {
      const backup = local.draft()
      // Resolve a PUT whose response was lost without sending it again.
      if (
        backup &&
        response.version > backup.version &&
        JSON.stringify(toLayout(backup.furniture)) === JSON.stringify(toLayout(items.value))
      ) {
        local.finalize(backup.furniture)
        message.value = '이전에 저장한 배치와 활동 연결을 복구했어요.'
      }
      items.value = fromLayout(response.layout, local.bindings())
      hasRecovery.value = !!local.draft()
    } catch (error) {
      localFailure.value = true
      failure(error, '로컬 연결 정보를 읽지 못했어요. 원본은 보존됩니다.')
    }
    try {
      hasOriginal.value = !!local.original()
    } catch {
      message.value = '기존 브라우저 배치를 읽지 못했어요. 가져오지 않고 원본을 보존합니다.'
    }
  } catch (error) {
    if (!alive || current !== requestId) return
    loadFailed.value = true
    failure(error, '배치를 불러오지 못했어요. 다시 불러와주세요.')
  } finally {
    if (alive && current === requestId) loading.value = false
  }
}
function beforeUnload(e: BeforeUnloadEvent) {
  if (dirty.value || saving.value) {
    e.preventDefault()
    e.returnValue = ''
  }
}
function canLeave() {
  if (saving.value) {
    message.value = '저장 중이에요. 완료 후 이동해주세요.'
    return false
  }
  return (
    !dirty.value || window.confirm('저장하지 않은 가구 배치가 있습니다. 저장하지 않고 나갈까요?')
  )
}
defineExpose({ canLeave, dirty })
function start() {
  if (locked.value) return
  draft.value = JSON.parse(JSON.stringify(items.value))
  selected.value = draft.value[0]?.id ?? null
  editing.value = true
  message.value = ''
}
function cancel() {
  if (saving.value || props.blocked) return
  editing.value = false
  draft.value = []
  selected.value = null
  message.value = ''
  conflict.value = false
  latest.value = null
  previewing.value = false
}
async function save() {
  if (locked.value || conflict.value || !editing.value) return
  if (!validRoom(draft.value)) {
    message.value = '가구 배치를 확인해주세요.'
    return
  }
  const current = requestId,
    spaceId = props.spaceId
  const snapshot = JSON.parse(JSON.stringify(draft.value)) as Furniture[]
  saving.value = true
  try {
    local.prepare(version.value, snapshot)
    hasRecovery.value = true
    const response = readResponse(
      await apiRequest(`/api/spaces/${spaceId}/room`, {
        method: 'PUT',
        body: JSON.stringify({ version: version.value, layout: toLayout(snapshot) }),
      }),
      spaceId,
    )
    if (!alive || current !== requestId) return
    items.value = fromLayout(response.layout)
    version.value = response.version
    updatedAt.value = response.updatedAt
    editing.value = false
    loadFailed.value = false
    try {
      local.finalize(snapshot)
      items.value = fromLayout(response.layout, local.bindings())
      hasRecovery.value = false
      message.value = '배치를 저장했어요. 공간 멤버와 같은 배치를 볼 수 있어요.'
    } catch {
      items.value = snapshot
      localFailure.value = true
      message.value =
        '공간 배치는 저장됐지만 활동 연결 보존을 완료하지 못했어요. 복구 초안은 남아 있습니다. 다시 불러와주세요.'
    }
  } catch (error) {
    if (!alive || current !== requestId) return
    if (error instanceof ApiError && error.status === 409) {
      conflict.value = true
      message.value = ''
    } else failure(error, '저장하지 못했어요. 편집 내용은 그대로 남아 있어요.')
  } finally {
    if (alive && current === requestId) saving.value = false
  }
}
function importDraft(recovery = false) {
  if (locked.value || editing.value) return
  try {
    const backup = recovery ? local.draft() : null
    const furniture = recovery ? backup?.furniture : local.original()
    if (!furniture) return
    draft.value = JSON.parse(JSON.stringify(furniture)) as Furniture[]
    editing.value = true
    selected.value = draft.value[0]?.id ?? null
    conflict.value = !!backup && backup.version !== version.value
    message.value = '편집 초안으로 가져왔어요. 배치 저장 전에는 공간에 반영되지 않아요.'
  } catch (error) {
    failure(error, '배치를 가져오지 못했어요. 원본은 보존됩니다.')
  }
}
async function inspectLatest() {
  if (loading.value || saving.value) return
  const current = requestId,
    spaceId = props.spaceId
  loading.value = true
  controller?.abort()
  controller = new AbortController()
  try {
    const response = readResponse(
      await apiRequest(`/api/spaces/${spaceId}/room`, { signal: controller.signal }),
      spaceId,
    )
    if (!alive || current !== requestId) return
    latest.value = response
    previewing.value = true
    message.value = ''
  } catch (error) {
    if (alive && current === requestId)
      failure(error, '최신 배치를 불러오지 못했어요. 초안은 유지됩니다.')
  } finally {
    if (alive && current === requestId) loading.value = false
  }
}
function editLatest() {
  if (!latest.value || loading.value || !canEdit.value || props.blocked) return
  if (!window.confirm('현재 초안을 백업하고 최신 배치로 새 편집을 시작할까요?')) return
  try {
    local.archive(version.value, draft.value)
    items.value = fromLayout(latest.value.layout, local.bindings())
    version.value = latest.value.version
    updatedAt.value = latest.value.updatedAt
    previewing.value = false
    conflict.value = false
    latest.value = null
    start()
    message.value = '이전 초안을 백업했어요. 최신 배치에 변경 내용을 다시 적용해주세요.'
  } catch (error) {
    failure(error, '초안을 백업하지 못했어요. 기존 초안은 유지됩니다.')
  }
}
function move(id: string, x: number, y: number) {
  if (locked.value || conflict.value) return
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
  if (locked.value || conflict.value) return
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
  if (locked.value || conflict.value) return
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
  if (locked.value || conflict.value) return
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
  if (props.blocked || editing.value || loading.value || saving.value) return
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
    <p class="room-storage-note">
      가구 배치는 공간 멤버에게 공유돼요. 도구의 내용과 활동 연결은 아직 이 브라우저에서만 사용할 수
      있어요.
    </p>
    <p v-if="loading" class="room-message" role="status">공간의 가구 배치를 불러오고 있어요.</p>
    <p v-else-if="!canEdit" class="room-message">가구 배치는 소유자와 관리자가 수정할 수 있어요.</p>
    <div
      v-if="
        canEdit &&
        !hideImports &&
        !editing &&
        !loading &&
        !loadFailed &&
        !localFailure &&
        (hasOriginal || hasRecovery)
      "
      class="room-message"
    >
      <p>
        이 브라우저에 이전 배치 또는 복구 초안이 있어요. 가져오면 편집 초안으로 열립니다. 원본은
        남겨둡니다.
      </p>
      <button v-if="hasOriginal" :disabled="locked" @click="importDraft()">
        기존 배치 가져오기
      </button>
      <button v-if="hasRecovery" :disabled="locked" @click="importDraft(true)">
        복구 초안 가져오기
      </button>
      <button :disabled="locked" @click="hideImports = true">지금은 안 하기</button>
    </div>
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
          ><button :disabled="blocked || saving || loading" @click="cancel">변경 취소</button
          ><button class="room-primary" :disabled="editLocked" @click="save">
            {{ saving ? '저장 중…' : '배치 저장' }}
          </button></template
        ><template v-else
          ><button :disabled="blocked" @click="emit('invite')">멤버 초대</button
          ><button v-if="canEdit" class="room-primary" :disabled="locked" @click="start">
            방 꾸미기
          </button></template
        >
      </div>
    </div>
    <p v-if="message" class="room-message" role="status">{{ message }}</p>
    <button v-if="loadFailed || localFailure" :disabled="loading || saving" @click="load">
      다시 불러오기
    </button>
    <div v-if="conflict" class="room-message room-conflict" role="alert">
      <strong>다른 멤버가 배치를 변경했어요.</strong>
      <p>
        편집 중인 초안은 그대로 남아 있어요. 최신 배치를 확인한 뒤 변경 내용을 다시 적용해주세요.
      </p>
      <button :disabled="loading || saving || blocked" @click="inspectLatest">
        최신 배치 확인
      </button>
      <button v-if="latest" :disabled="loading || saving" @click="previewing = false">
        내 초안으로 돌아가기
      </button>
      <button
        v-if="previewing"
        :disabled="loading || saving || !canEdit || blocked"
        @click="editLatest"
      >
        최신 배치로 새 편집
      </button>
      <p v-if="previewing">최신 저장 배치 미리보기 · 내 초안은 유지돼요.</p>
    </div>
    <div class="room-workspace" :class="{ editing }">
      <aside v-if="editing" class="room-panel inventory">
        <h2>가구 추가</h2>
        <div class="furniture-list">
          <button
            v-for="(name, kind) in furnitureNames"
            :key="kind"
            :disabled="editLocked"
            @click="add(kind)"
          >
            <img v-if="previews[kind]" :src="previews[kind]" alt="" /><span>{{ name }}</span>
          </button>
        </div>
      </aside>
      <section class="room-stage">
        <RoomCanvas
          :items="visibleItems"
          :editing="editing && !editLocked"
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
          ><template v-if="previewing"
            ><h2>최신 저장 배치</h2>
            <p>가구 {{ latestItems.length }}개 · 조회만 하고 있어요.</p>
            <p class="room-muted">
              내 초안은 그대로 남아 있어요. 초안으로 돌아가거나 최신 배치로 새 편집을 시작할 수
              있어요.
            </p></template
          ><template v-else-if="object"
            ><h2>{{ furnitureNames[object.kind] }}</h2>
            <img
              v-if="previews[object.kind]"
              class="selected-preview"
              :src="previews[object.kind]"
              alt="선택한 가구"
            />
            <p class="room-muted">차지하는 격자 · {{ object.w }} × {{ object.h }}칸</p>
            <label
              >연결 대상<select v-model="object.event" :disabled="editLocked" @change="target">
                <option :value="undefined">공간 전체</option>
                <option v-for="event in activities" :key="event.id" :value="event.id">
                  {{ event.title }}
                </option>
              </select></label
            >
            <p v-if="object.event" class="room-muted">
              이 활동 연결은 현재 계정·브라우저에서만 사용해요. 다른 멤버에게는 장식으로 보여요.
            </p>
            <label
              >연결할 기능<select v-model="object.module" :disabled="editLocked" @change="binding">
                <option value="">장식으로만 사용</option>
                <option value="activities">이벤트 목록</option>
                <option v-for="m in connections" :key="m.id" :value="m.id">{{ m.label }}</option>
              </select></label
            ><label
              >표시 이름<input
                v-model="object.label"
                maxlength="24"
                :disabled="editLocked"
                placeholder="예: 여행 정산"
            /></label>
            <p class="room-muted">현재 위치: {{ object.x + 1 }}, {{ object.y + 1 }}</p>
            <div class="room-keypad">
              <button aria-label="가구 위로 이동" :disabled="editLocked" @click="nudge(0, -1)">
                ↑</button
              ><button aria-label="가구 왼쪽으로 이동" :disabled="editLocked" @click="nudge(-1, 0)">
                ←</button
              ><button
                aria-label="가구 오른쪽으로 이동"
                :disabled="editLocked"
                @click="nudge(1, 0)"
              >
                →</button
              ><button aria-label="가구 아래로 이동" :disabled="editLocked" @click="nudge(0, 1)">
                ↓
              </button>
            </div>
            <button :disabled="editLocked" @click="rotate">↻ 90° 회전</button
            ><button :disabled="editLocked" @click="remove">가구 치우기</button>
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
    <p class="room-storage-note">
      배치 저장은 공간에 반영돼요. 낮·저녁 설정은 나에게만 적용돼요.
      <span v-if="updatedAt"> 마지막 저장: {{ new Date(updatedAt).toLocaleString('ko-KR') }}</span>
      <span v-else-if="!loading && !loadFailed && version === 0"> 아직 저장한 배치가 없어요.</span>
    </p>
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
.room-stage {
  min-width: 0;
  overflow: hidden;
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
.room-message button {
  margin: 6px 8px 0 0;
}
.room-conflict {
  border-left: 4px solid #b76863;
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
