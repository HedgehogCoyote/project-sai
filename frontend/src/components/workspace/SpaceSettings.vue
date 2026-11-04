<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError } from '@/services/api'
import { api, PALETTES, RELATIONS, type Relation, type SpaceDetail } from '@/services/workspace'

const props = defineProps<{ spaceId: number }>()
const emit = defineEmits<{ sessionExpired: [error: ApiError]; changed: [detail: SpaceDetail]; left: [] }>()
const detail = ref<SpaceDetail | null>(null),
  title = ref(''),
  relation = ref<Relation>('FRIENDS'),
  palette = ref(0),
  busy = ref(false),
  error = ref(''),
  ok = ref(''),
  confirmText = ref(''),
  danger = ref<'leave' | 'delete' | null>(null)

function fail(e: unknown, fallback: string) {
  if (e instanceof ApiError && e.status === 401) emit('sessionExpired', e)
  error.value = e instanceof Error ? e.message : fallback
}
function fill(d: SpaceDetail) {
  detail.value = d
  title.value = d.title
  relation.value = d.relation
  const i = PALETTES.findIndex((p) => p.primary === d.colorPrimary && p.secondary === d.colorSecondary)
  palette.value = i < 0 ? 0 : i
}
onMounted(async () => {
  try {
    fill(await api.space(props.spaceId))
  } catch (e) {
    fail(e, '공간 설정을 불러오지 못했습니다.')
  }
})
async function save() {
  busy.value = true
  error.value = ''
  ok.value = ''
  try {
    const p = PALETTES[palette.value]!
    const d = await api.updateSpace(props.spaceId, {
      title: title.value.trim(),
      relation: relation.value,
      colorPrimary: p.primary,
      colorSecondary: p.secondary,
    })
    fill(d)
    ok.value = '공간 설정을 저장했습니다.'
    emit('changed', d)
  } catch (e) {
    fail(e, '공간 설정을 저장하지 못했습니다.')
  } finally {
    busy.value = false
  }
}
async function confirmDanger() {
  busy.value = true
  error.value = ''
  try {
    if (danger.value === 'delete') await api.deleteSpace(props.spaceId)
    else await api.leave(props.spaceId)
    emit('left')
  } catch (e) {
    fail(e, danger.value === 'delete' ? '공간을 삭제하지 못했습니다.' : '공간을 나가지 못했습니다.')
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <section class="ws-section" aria-label="공간 설정">
    <div class="ws-head"><h2>공간 설정</h2></div>
    <p v-if="error" class="ws-error" role="alert">{{ error }}</p>
    <p v-if="ok" class="ws-ok" role="status">{{ ok }}</p>
    <form v-if="detail" class="ws-form" @submit.prevent="save">
      <fieldset :disabled="busy || detail.myRole === 'MEMBER'" class="ws-form plain">
        <label>공간 이름<input v-model="title" maxlength="50" required /></label>
        <div class="ws-label">관계
          <div class="ws-choice" role="radiogroup" aria-label="관계">
            <button v-for="r in RELATIONS" :key="r.id" type="button" role="radio" :aria-checked="relation === r.id" @click="relation = r.id">
              <strong>{{ r.label }}</strong><span>{{ r.hint }}</span>
            </button>
          </div>
        </div>
        <div class="ws-label">공간 색 · 방과 목록에 쓰입니다
          <div class="ws-swatches" role="radiogroup" aria-label="공간 색">
            <button v-for="(p, i) in PALETTES" :key="p.label" class="ws-swatch" type="button" role="radio" :aria-checked="palette === i" @click="palette = i">
              <svg class="ws-mark" viewBox="0 0 72 54" aria-hidden="true"><circle cx="24" cy="27" r="20" :fill="p.primary" /><circle cx="48" cy="27" r="20" :fill="p.secondary" style="mix-blend-mode: multiply" /></svg>
              {{ p.label }}
            </button>
          </div>
        </div>
        <div v-if="detail.myRole !== 'MEMBER'" class="ws-actions"><button class="ws-btn primary" type="submit">설정 저장</button></div>
        <p v-else class="ws-hint">설정은 소유자와 관리자가 바꿀 수 있습니다.</p>
      </fieldset>
    </form>
    <div v-if="detail" class="ws-danger">
      <strong>공간 나가기와 삭제</strong>
      <p class="ws-hint">
        {{ detail.myRole === 'OWNER' ? '소유자는 다른 멤버에게 소유권을 넘긴 뒤 나갈 수 있습니다. 혼자 남은 공간에서 나가면 공간이 삭제됩니다.' : '나가면 이 공간의 내용을 볼 수 없습니다. 다시 들어오려면 초대를 받아야 합니다.' }}
      </p>
      <div v-if="!danger" class="ws-actions">
        <button class="ws-btn danger" type="button" :disabled="busy" @click="(danger = 'leave'), (confirmText = '')">공간 나가기</button>
        <button v-if="detail.myRole === 'OWNER'" class="ws-btn danger" type="button" :disabled="busy" @click="(danger = 'delete'), (confirmText = '')">공간 삭제</button>
      </div>
      <form v-else class="ws-form" @submit.prevent="confirmDanger">
        <label>
          {{ danger === 'delete' ? '삭제하면 활동, 사진, 기록이 모두 지워지고 되돌릴 수 없습니다.' : '정말 나갈까요?' }}
          확인하려면 공간 이름 "{{ detail.title }}"을 입력해 주세요.
          <input v-model="confirmText" :aria-label="`공간 이름 ${detail.title} 입력`" />
        </label>
        <div class="ws-actions">
          <button class="ws-btn danger" type="submit" :disabled="busy || confirmText.trim() !== detail.title">
            {{ danger === 'delete' ? '공간 삭제' : '나가기' }}
          </button>
          <button class="ws-btn" type="button" :disabled="busy" @click="danger = null">취소</button>
        </div>
      </form>
    </div>
  </section>
</template>

<style scoped>
fieldset.plain { margin: 0; padding: 0; border: 0; min-width: 0; }
</style>
