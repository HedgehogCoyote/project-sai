<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ApiError } from '@/services/api'
import { api, roleLabel, type Role, type SpaceMember } from '@/services/workspace'
import InviteLinks from './InviteLinks.vue'

const props = defineProps<{ spaceId: number; myUserId: number; myRole: Role }>()
const emit = defineEmits<{ sessionExpired: [error: ApiError]; changed: [] }>()
const members = ref<SpaceMember[]>([]),
  loading = ref(true),
  busy = ref(false),
  error = ref(''),
  ok = ref(''),
  nickname = ref(''),
  confirmTransfer = ref<SpaceMember | null>(null)

const me = computed(() => members.value.find((m) => m.userId === props.myUserId))
const isOwner = computed(() => props.myRole === 'OWNER')
const canManage = computed(() => props.myRole !== 'MEMBER')
const canRemove = (m: SpaceMember) =>
  m.userId !== props.myUserId && m.role !== 'OWNER' && (isOwner.value || (props.myRole === 'MANAGER' && m.role === 'MEMBER'))

function fail(e: unknown, fallback: string) {
  if (e instanceof ApiError && e.status === 401) emit('sessionExpired', e)
  error.value = e instanceof Error ? e.message : fallback
}
async function load() {
  loading.value = true
  try {
    members.value = await api.members(props.spaceId)
    nickname.value = me.value?.nickname ?? ''
  } catch (e) {
    fail(e, '멤버를 불러오지 못했습니다.')
  } finally {
    loading.value = false
  }
}
async function run(action: () => Promise<unknown>, done: string, fallback: string) {
  busy.value = true
  error.value = ''
  ok.value = ''
  try {
    await action()
    ok.value = done
    await load()
    emit('changed')
  } catch (e) {
    fail(e, fallback)
  } finally {
    busy.value = false
  }
}
function setRole(m: SpaceMember, role: Role) {
  if (role === 'OWNER') {
    confirmTransfer.value = m
    return
  }
  void run(() => api.setRole(props.spaceId, m.userId, role), `${m.nickname || m.name}님의 역할을 바꿨습니다.`, '역할을 바꾸지 못했습니다.')
}
function transfer() {
  const m = confirmTransfer.value
  confirmTransfer.value = null
  if (m) void run(() => api.setRole(props.spaceId, m.userId, 'OWNER'), `${m.nickname || m.name}님에게 소유권을 넘겼습니다.`, '소유권을 넘기지 못했습니다.')
}
function remove(m: SpaceMember) {
  void run(() => api.removeMember(props.spaceId, m.userId), `${m.nickname || m.name}님을 내보냈습니다.`, '멤버를 내보내지 못했습니다.')
}
function saveNickname() {
  void run(() => api.nickname(props.spaceId, nickname.value.trim()), '이 공간에서 쓸 이름을 저장했습니다.', '이름을 저장하지 못했습니다.')
}
onMounted(load)
</script>

<template>
  <div class="ws-section">
    <section class="ws-section" aria-label="멤버">
      <div class="ws-head"><h2>멤버 {{ members.length }}명</h2></div>
      <p v-if="error" class="ws-error" role="alert">{{ error }}</p>
      <p v-if="ok" class="ws-ok" role="status">{{ ok }}</p>
      <p v-if="loading" class="ws-meta" role="status">멤버를 불러오는 중...</p>
      <div v-else class="ws-list">
        <div v-for="m in members" :key="m.userId" class="ws-row">
          <div class="ws-grow">
            <strong>{{ m.nickname || m.name }}<template v-if="m.userId === myUserId"> (나)</template></strong>
            <small>@{{ m.loginId }}<template v-if="m.nickname"> · {{ m.name }}</template></small>
          </div>
          <select
            v-if="isOwner && m.userId !== myUserId"
            :aria-label="`${m.nickname || m.name} 역할`"
            :value="m.role"
            :disabled="busy"
            @change="setRole(m, ($event.target as HTMLSelectElement).value as Role)"
          >
            <option value="MANAGER">관리자</option>
            <option value="MEMBER">멤버</option>
            <option value="OWNER">소유자로 넘기기</option>
          </select>
          <span v-else class="ws-badge" :class="{ strong: m.role === 'OWNER' }">{{ roleLabel(m.role) }}</span>
          <button v-if="canRemove(m)" class="ws-btn small danger" type="button" :disabled="busy" @click="remove(m)">내보내기</button>
        </div>
      </div>
      <div v-if="confirmTransfer" class="ws-danger" role="alertdialog" aria-label="소유권 넘기기 확인">
        <strong>{{ confirmTransfer.nickname || confirmTransfer.name }}님에게 소유권을 넘길까요?</strong>
        <p class="ws-hint">넘기면 나는 관리자가 되고, 공간 삭제와 역할 변경은 새 소유자만 할 수 있습니다.</p>
        <div class="ws-actions">
          <button class="ws-btn danger" type="button" @click="transfer">소유권 넘기기</button>
          <button class="ws-btn" type="button" @click="confirmTransfer = null">취소</button>
        </div>
      </div>
    </section>
    <form class="ws-form ws-card" @submit.prevent="saveNickname">
      <label>이 공간에서 쓸 내 이름<input v-model="nickname" maxlength="30" :placeholder="me?.name ?? ''" :disabled="busy" /></label>
      <p class="ws-hint">비워 두면 계정 이름을 씁니다. 다른 공간에는 영향을 주지 않습니다.</p>
      <div class="ws-actions"><button class="ws-btn" type="submit" :disabled="busy">이름 저장</button></div>
    </form>
    <InviteLinks v-if="canManage" :space-id="spaceId" @session-expired="emit('sessionExpired', $event)" />
    <slot />
  </div>
</template>

<style scoped>
select { min-height: 34px; border: 1px solid #d5d7dc; border-radius: 4px; padding: 4px 8px; background: #fff; font: inherit; font-size: 13px; }
</style>
