<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLogo from '@/components/AppLogo.vue'
import SpaceContent from '@/features/content/SpaceContent.vue'
import { ApiError } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { useSpacesStore, type SpaceRole } from '@/stores/spaces'

const auth = useAuthStore()
const spacesStore = useSpacesStore()
const route = useRoute()
const router = useRouter()
const contentView = ref<{ canLeave: () => boolean } | null>(null)
const createOpen = ref(false)
const createDialog = ref<HTMLDialogElement | null>(null)
const newSpaceTitle = ref('')
const inviteeUserId = ref<number | null>(null)
const pageError = ref('')
const createError = ref('')
const inviteError = ref('')
const inviteSuccess = ref('')
const selectedSpace = computed(() =>
  spacesStore.spaces.find((space) => String(space.spaceId) === route.query.space),
)

const canInvite = computed(
  () =>
    !!selectedSpace.value &&
    Number.isSafeInteger(inviteeUserId.value) &&
    Number(inviteeUserId.value) > 0,
)
function roleLabel(role: SpaceRole) {
  return { OWNER: '소유자', MANAGER: '관리자', MEMBER: '멤버' }[role]
}
watch(
  () => selectedSpace.value?.spaceId,
  () => {
    inviteeUserId.value = null
    inviteError.value = ''
    inviteSuccess.value = ''
  },
)
watch(createOpen, async (open) => {
  if (open) {
    await nextTick()
    createDialog.value?.showModal()
  }
})
async function navigate(spaceId?: number, view = 'home') {
  if (spacesStore.inviting) return
  await router.push({
    name: 'home',
    query:
      spaceId === undefined
        ? {}
        : { space: String(spaceId), ...(view === 'invite' ? { view } : {}) },
  })
}
async function handleApiError(error: unknown, fallback: string) {
  if (error instanceof ApiError && error.status === 401) {
    auth.clearSession()
    spacesStore.clearSpaces()
    await router.replace({ name: 'login', query: { redirect: route.fullPath } })
    return '로그인이 만료되었습니다. 다시 로그인해 주세요.'
  }
  return error instanceof ApiError ? error.message : fallback
}
async function loadSpaces() {
  pageError.value = ''
  try {
    await spacesStore.fetchMySpaces()
  } catch (error) {
    pageError.value = await handleApiError(error, '공간 목록을 불러오지 못했습니다.')
  }
}
function openCreateDialog() {
  newSpaceTitle.value = ''
  createError.value = ''
  createOpen.value = true
}
function closeCreateDialog() {
  if (!spacesStore.creating) createOpen.value = false
}
async function submitCreate() {
  const title = newSpaceTitle.value.trim()
  createError.value = ''
  if (!title || title.length > 50) {
    createError.value = '공간 이름은 1자 이상 50자 이하로 입력해 주세요.'
    return
  }
  try {
    const id = await spacesStore.createSpace(title)
    createOpen.value = false
    await navigate(id)
  } catch (error) {
    createError.value = await handleApiError(error, '공간을 만들지 못했습니다.')
  }
}
async function submitInvitation() {
  if (spacesStore.inviting) return
  inviteError.value = ''
  inviteSuccess.value = ''
  if (!canInvite.value || !selectedSpace.value || inviteeUserId.value === null) {
    inviteError.value = '초대할 사용자 ID를 양의 정수로 입력해 주세요.'
    return
  }
  const spaceId = selectedSpace.value.spaceId
  const userId = inviteeUserId.value
  try {
    await spacesStore.inviteUser(spaceId, userId)
    // 뒤로 가기로 공간을 바꿨다면 이전 공간의 결과를 표시하지 않는다.
    if (selectedSpace.value?.spaceId === spaceId) {
      inviteSuccess.value = `${userId}번 사용자에게 초대를 보냈습니다.`
      inviteeUserId.value = null
    }
  } catch (error) {
    const message = await handleApiError(error, '초대를 보내지 못했습니다.')
    if (selectedSpace.value?.spaceId === spaceId) inviteError.value = message
  }
}
async function logout() {
  if (contentView.value && !contentView.value.canLeave()) return
  pageError.value = ''
  try {
    await auth.logout()
    spacesStore.clearSpaces()
    await router.replace('/login')
  } catch (error) {
    pageError.value = await handleApiError(error, '로그아웃하지 못했습니다.')
  }
}
onMounted(loadSpaces)
</script>

<template>
  <div class="home-shell">
    <header class="site-header">
      <button
        class="brand-link"
        type="button"
        aria-label="모든 공간으로 이동"
        :disabled="spacesStore.inviting"
        @click="navigate()"
      >
        <AppLogo light />
      </button>
      <span class="brand-note">함께할 공간</span>
      <div class="account">
        <span>{{ auth.user?.name }} 님</span
        ><button type="button" :disabled="auth.pending || spacesStore.inviting" @click="logout">
          {{ auth.pending ? '로그아웃 중...' : '로그아웃' }}
        </button>
      </div>
    </header>
    <div class="workspace">
      <aside v-if="selectedSpace" class="space-sidebar" aria-label="공간 선택">
        <button
          class="space-link"
          :class="{ active: !selectedSpace }"
          type="button"
          :aria-current="!selectedSpace ? 'page' : undefined"
          :disabled="spacesStore.inviting"
          @click="navigate()"
        >
          모든 공간
        </button>
        <p class="sidebar-label">참여 중인 공간</p>
        <button
          v-for="space in spacesStore.spaces"
          :key="space.spaceId"
          class="space-link"
          :class="{ active: selectedSpace?.spaceId === space.spaceId }"
          type="button"
          :aria-current="selectedSpace?.spaceId === space.spaceId ? 'page' : undefined"
          :disabled="spacesStore.inviting"
          @click="navigate(space.spaceId)"
        >
          {{ space.title }}
        </button>
        <p v-if="!spacesStore.loading && !spacesStore.hasSpaces" class="sidebar-empty">
          참여 중인 공간이 없습니다.
        </p>
        <button
          class="sidebar-create secondary-button"
          type="button"
          :disabled="spacesStore.inviting"
          @click="openCreateDialog"
        >
          ＋ 새 공간 만들기
        </button>
      </aside>
      <main class="home-main">
        <div class="breadcrumb">
          <button type="button" :disabled="spacesStore.inviting" @click="navigate()">내 공간</button
          ><template v-if="selectedSpace"
            ><span aria-hidden="true">/</span><span>{{ selectedSpace.title }}</span></template
          >
        </div>
        <p v-if="pageError" class="status-message status-message--error" role="alert">
          {{ pageError }} <button type="button" @click="loadSpaces">다시 시도</button>
        </p>
        <div v-if="spacesStore.loading" class="loading-state" role="status">
          공간을 불러오는 중...
        </div>
        <template v-else-if="!selectedSpace">
          <div class="spaces-heading">
            <h1>내 공간</h1>
            <button
              class="secondary-button"
              type="button"
              :disabled="spacesStore.inviting"
              @click="openCreateDialog"
            >
              ＋ 새 공간 만들기
            </button>
          </div>
          <p class="description">참여 중인 공간을 선택해 들어가세요.</p>
          <div
            v-if="spacesStore.hasSpaces"
            class="space-table"
            role="table"
            aria-label="내 공간 목록"
          >
            <div class="space-row space-row--header" role="row">
              <span role="columnheader">공간 이름</span><span role="columnheader">내 역할</span
              ><span role="columnheader">참여 인원</span><span role="columnheader">이동</span>
            </div>
            <div
              v-for="(space, index) in spacesStore.spaces"
              :key="space.spaceId"
              class="space-row"
              role="row"
            >
              <div role="cell">
                <div class="space-cover" aria-hidden="true">
                  <svg viewBox="0 0 240 140">
                    <path
                      d="M120 8L20 56V99L120 139L220 92V51Z"
                      :fill="['#c8adbb', '#bfd0be', '#c7bfce'][index % 3]"
                    />
                    <path d="M20 99L120 48L220 92L120 139Z" fill="#dfbd94" />
                    <path d="M43 57L79 40V73L43 89Z" fill="#eee5ce" />
                    <path d="M51 58L72 48V68L51 80Z" fill="#b8ccc9" />
                    <path d="M59 85L92 69L133 90L99 111L59 94Z" fill="#8c6f8e" />
                    <path d="M60 77L92 62L92 83L60 98Z" fill="#a58aa0" />
                    <path d="M145 70L177 57L202 70L171 84Z" fill="#d1a074" />
                    <path d="M145 70V96M171 84V111M201 70V97" stroke="#b18761" stroke-width="5" />
                    <path
                      d="M187 49V22M187 34Q164 24 174 11M187 36Q209 19 203 10"
                      stroke="#7b9677"
                      stroke-width="6"
                      stroke-linecap="round"
                    />
                    <path d="M177 43L198 43L194 61L181 61Z" fill="#e6cfb3" />
                  </svg>
                </div>
                <h2>{{ space.title }}</h2>
                <p class="space-id">공간 ID {{ space.spaceId }}</p>
              </div>
              <span role="cell"
                ><span class="role-badge">{{ roleLabel(space.role) }}</span></span
              ><span role="cell">{{ space.spaceMemberCount }}명</span>
              <div role="cell">
                <button
                  class="secondary-button"
                  type="button"
                  :aria-label="`${space.title} 들어가기`"
                  @click="navigate(space.spaceId)"
                >
                  들어가기
                </button>
              </div>
            </div>
          </div>
          <div v-else class="empty-state">
            <h2>아직 참여 중인 공간이 없습니다</h2>
            <p>왼쪽의 새 공간 만들기로 첫 공간을 시작하세요.</p>
          </div>
        </template>
        <SpaceContent
          ref="contentView"
          v-else
          :key="`${auth.user?.loginId}:${selectedSpace.spaceId}`"
          :space="selectedSpace"
          :account="{ loginId: auth.user?.loginId ?? '', name: auth.user?.name ?? '나' }"
          :blocked="spacesStore.inviting"
        >
          <template #invitation>
            <section aria-labelledby="invitation-title">
              <h2 id="invitation-title">사용자 초대</h2>
              <p class="description">현재 공간에 함께할 사용자를 초대합니다.</p>
              <form class="invitation-form" @submit.prevent="submitInvitation">
                <div class="field">
                  <span class="field-label">초대할 공간</span>
                  <div class="fixed-space">
                    <strong>{{ selectedSpace.title }}</strong
                    ><span>ID {{ selectedSpace.spaceId }}</span>
                  </div>
                  <p class="field-hint">초대할 공간을 바꾸려면 왼쪽에서 다른 공간을 선택하세요.</p>
                </div>
                <div class="field">
                  <label for="inviteeUserId">사용자 ID</label
                  ><input
                    id="inviteeUserId"
                    v-model.number="inviteeUserId"
                    type="number"
                    min="1"
                    step="1"
                    placeholder="예: 24"
                    required
                    :disabled="spacesStore.inviting"
                  />
                  <p class="field-hint">로그인 아이디가 아닌 숫자 사용자 ID를 입력하세요.</p>
                </div>
                <button
                  class="primary-button invitation-submit"
                  type="submit"
                  :disabled="!canInvite || spacesStore.inviting"
                >
                  {{ spacesStore.inviting ? '초대 중...' : '초대 보내기' }}
                </button>
                <p v-if="spacesStore.inviting" class="field-hint" role="status">
                  초대를 보내고 있습니다. 잠시 기다려 주세요.
                </p>
                <p v-if="inviteError" class="status-message status-message--error" role="alert">
                  {{ inviteError }}
                </p>
                <p
                  v-if="inviteSuccess"
                  class="status-message status-message--success"
                  role="status"
                >
                  {{ inviteSuccess }}
                </p>
              </form>
            </section>
          </template>
        </SpaceContent>
      </main>
    </div>
    <dialog
      v-if="createOpen"
      ref="createDialog"
      class="create-dialog"
      aria-labelledby="create-space-title"
      @cancel.prevent="closeCreateDialog"
      @click.self="closeCreateDialog"
    >
      <div class="dialog-heading">
        <h2 id="create-space-title">새 공간 만들기</h2>
        <button
          class="close-button"
          type="button"
          aria-label="닫기"
          :disabled="spacesStore.creating"
          @click="closeCreateDialog"
        >
          ×
        </button>
      </div>
      <p class="description">함께할 공간의 이름을 정해 주세요.</p>
      <form @submit.prevent="submitCreate">
        <div class="field">
          <div class="field__label-row">
            <label for="spaceTitle">공간 이름</label
            ><small>{{ newSpaceTitle.trim().length }}/50</small>
          </div>
          <input
            id="spaceTitle"
            v-model="newSpaceTitle"
            maxlength="50"
            autofocus
            placeholder="예: 우리 가족"
            required
            :disabled="spacesStore.creating"
          />
        </div>
        <p v-if="createError" class="form-error" role="alert">{{ createError }}</p>
        <div class="dialog-actions">
          <button
            class="secondary-button"
            type="button"
            :disabled="spacesStore.creating"
            @click="closeCreateDialog"
          >
            취소</button
          ><button class="primary-button" type="submit" :disabled="spacesStore.creating">
            {{ spacesStore.creating ? '만드는 중...' : '공간 만들기' }}
          </button>
        </div>
      </form>
    </dialog>
  </div>
</template>

<style scoped>
.home-hero {
  display: grid;
  grid-template-columns: 1fr 1fr;
  align-items: center;
  gap: 24px;
  padding: 36px;
  margin-bottom: 42px;
  border: 1px solid #ece7f3;
  border-radius: 18px;
  background: #f8f6fc;
  color: #574473;
}
.hero-eyebrow {
  margin: 0 0 18px;
  font-size: 11px;
  letter-spacing: 0.08em;
}
.hero-copy h2 {
  margin: 0;
  font-size: clamp(26px, 3vw, 40px);
  letter-spacing: -0.055em;
  line-height: 1.35;
  color: var(--navy-900);
}
.hero-copy > p:last-of-type {
  margin: 18px 0 24px;
  color: #7c718a;
  font-size: 13px;
  line-height: 1.9;
}
.hero-create {
  display: inline-flex;
  gap: 24px;
  align-items: center;
  padding: 12px 18px;
  border: 0;
  border-radius: 6px;
  background: #574173;
  color: white;
  cursor: pointer;
  font-size: 13px;
}
.hero-create:hover {
  background: #34234f;
}
@media (max-width: 1100px) {
  .home-hero {
    padding: 28px;
    gap: 12px;
  }
}
@media (max-width: 700px) {
  .home-hero {
    grid-template-columns: 1fr;
    padding: 26px 22px;
  }
  .home-hero :deep(.union-scene) {
    max-width: 400px;
    margin: 12px auto 0;
  }
}
.home-shell {
  min-height: 100svh;
  background: #fff;
}
.site-header {
  height: 76px;
  padding: 0 36px;
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: center;
  gap: 18px;
}
.brand-link {
  border: 0;
  padding: 0;
  background: none;
  cursor: pointer;
}
.brand-note {
  color: var(--text-muted);
  font-size: 13px;
}
.account {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 13px;
}
.account button {
  border: 0;
  border-left: 1px solid var(--line);
  padding-left: 20px;
  color: var(--text);
  background: none;
  cursor: pointer;
}
.workspace {
  display: grid;
  grid-template-columns: 232px minmax(0, 1fr);
  min-height: calc(100svh - 76px);
}
.space-sidebar {
  border-right: 1px solid var(--line);
  padding: 30px 20px;
  background: #fafafa;
}
.space-link {
  display: block;
  width: 100%;
  border: 0;
  border-radius: 4px;
  padding: 13px 12px;
  margin: 4px 0;
  background: none;
  color: #44474b;
  font-size: 14px;
  text-align: left;
  overflow-wrap: anywhere;
  cursor: pointer;
}
.space-link:hover {
  background: #f0f0f2;
}
.space-link.active {
  color: var(--navy-900);
  background: #eeebf2;
  font-weight: 700;
}
.sidebar-label {
  margin: 28px 12px 12px;
  color: var(--text-muted);
  font-size: 12px;
}
.sidebar-empty {
  margin: 12px;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.7;
}
.sidebar-create {
  width: 100%;
  margin-top: 22px;
}
.home-main {
  min-width: 0;
  max-width: 1320px;
  padding: 38px 56px 80px;
}
.breadcrumb {
  min-width: 0;
  display: flex;
  gap: 15px;
  margin-bottom: 24px;
  color: var(--text-muted);
  font-size: 12px;
  overflow-wrap: anywhere;
}
.breadcrumb button {
  border: 0;
  padding: 0;
  background: none;
  color: inherit;
  cursor: pointer;
}
h1 {
  margin: 0;
  font-size: 29px;
  letter-spacing: -1px;
  overflow-wrap: anywhere;
}
h2 {
  margin: 0 0 18px;
  font-size: 18px;
}
.description {
  margin: 12px 0 0;
  color: var(--text-muted);
  font-size: 14px;
  line-height: 1.7;
}
.space-table {
  margin-top: 32px;
  border-top: 1px solid #393a3c;
}
.space-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 110px 100px 100px;
  gap: 12px;
  align-items: center;
  padding: 24px 18px;
  border-bottom: 1px solid var(--line);
  font-size: 14px;
}
.space-row--header {
  padding: 14px 18px;
  background: #fafafa;
  color: var(--text-muted);
  font-size: 12px;
}
.space-row h2 {
  margin: 0;
  font-size: 16px;
  overflow-wrap: anywhere;
}
.space-id {
  margin: 7px 0 0;
  color: var(--text-soft);
  font-size: 12px;
}
.role-badge {
  padding: 5px 8px;
  border-radius: 3px;
  background: #f2eff5;
  color: #655378;
  font-size: 12px;
}
.secondary-button {
  min-height: 42px;
  border: 1px solid #d9dade;
  border-radius: 4px;
  padding: 10px 16px;
  background: #fff;
  color: #34363a;
  font-size: 14px;
  cursor: pointer;
}
.secondary-button:hover:not(:disabled) {
  border-color: var(--navy-900);
  color: var(--navy-900);
}
button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}
.loading-state,
.empty-state {
  margin-top: 32px;
  padding: 50px 24px;
  border-top: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
  color: var(--text-muted);
  line-height: 1.8;
}
.empty-state h2 {
  color: var(--text);
}
.space-tabs {
  display: flex;
  gap: 30px;
  margin: 28px 0 36px;
  border-bottom: 1px solid var(--line);
}
.space-tabs button {
  border: 0;
  border-bottom: 3px solid transparent;
  padding: 17px 2px;
  background: none;
  color: var(--text-muted);
  font-size: 14px;
  cursor: pointer;
}
.space-tabs button.active {
  border-bottom-color: var(--navy-900);
  color: var(--navy-900);
  font-weight: 700;
}
.space-stats {
  margin: 32px 0 0;
  display: flex;
  border-top: 1px solid #393a3c;
  border-bottom: 1px solid var(--line);
}
.space-stats > div {
  flex: 1;
  min-width: 0;
  padding: 25px 20px;
  border-right: 1px solid var(--line);
}
.space-stats > div:last-child {
  border: 0;
}
.space-stats dt {
  color: var(--text-muted);
  font-size: 12px;
}
.space-stats dd {
  margin: 14px 0 0;
  font-size: 22px;
  font-weight: 600;
}
.invite-intro {
  max-width: 730px;
  margin-top: 42px;
}
.invite-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 26px;
  border: 1px solid var(--line);
  border-radius: 4px;
}
.invite-box strong {
  font-size: 15px;
}
.invite-box p {
  margin: 8px 0 0;
  color: var(--text-muted);
  font-size: 14px;
  line-height: 1.7;
}
.invite-box button {
  flex-shrink: 0;
}
.invitation-form {
  max-width: 560px;
  display: grid;
  gap: 26px;
  margin-top: 28px;
  padding-top: 28px;
  border-top: 1px solid #393a3c;
}
.fixed-space {
  padding: 14px;
  background: #f8f8f9;
  border: 1px solid var(--line);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  font-size: 14px;
}
.fixed-space strong {
  font-weight: 400;
  overflow-wrap: anywhere;
}
.fixed-space span {
  color: var(--text-soft);
  white-space: nowrap;
}
.field-hint {
  margin: 4px 0 0;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.7;
}
.invitation-submit {
  width: fit-content;
}
.status-message {
  margin: 0;
  padding: 14px 16px;
  border-radius: 4px;
  font-size: 14px;
  line-height: 1.7;
  overflow-wrap: anywhere;
}
.status-message--error {
  color: var(--danger);
  background: #fff2f2;
}
.status-message--success {
  color: #25623e;
  background: #eff7f1;
}
.status-message button {
  margin-left: 8px;
  border: 0;
  padding: 4px 8px;
  color: inherit;
  background: none;
  text-decoration: underline;
  cursor: pointer;
}
.home-main > .status-message {
  margin-bottom: 24px;
}
.create-dialog {
  width: min(460px, calc(100% - 32px));
  padding: 28px;
  border: 1px solid var(--line);
  border-radius: 6px;
  color: var(--text);
  background: #fff;
  box-shadow: 0 10px 40px #0002;
}
.create-dialog::backdrop {
  background: #0006;
}
.dialog-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.dialog-heading h2 {
  margin: 0;
  font-size: 22px;
}
.close-button {
  border: 0;
  padding: 0 4px;
  background: none;
  font-size: 24px;
  cursor: pointer;
}
.create-dialog form {
  display: grid;
  gap: 18px;
  margin-top: 28px;
}
.create-dialog small {
  color: var(--text-soft);
  font-size: 12px;
}
.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
}
.dialog-actions .primary-button {
  width: auto;
}
@media (max-width: 1000px) {
  .home-main {
    padding: 32px;
  }
  .workspace {
    grid-template-columns: 200px minmax(0, 1fr);
  }
  .space-sidebar {
    padding: 24px 14px;
  }
  .space-row {
    grid-template-columns: minmax(0, 1fr) 75px 65px 92px;
    padding: 20px 8px;
    gap: 8px;
  }
  .invite-box {
    align-items: flex-start;
    flex-direction: column;
  }
}
@media (max-width: 700px) {
  .site-header {
    height: 64px;
    padding: 0 20px;
  }
  .brand-note {
    display: none;
  }
  .account {
    gap: 12px;
  }
  .account button {
    padding-left: 12px;
  }
  .workspace {
    display: block;
    min-height: calc(100svh - 64px);
  }
  .space-sidebar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 6px;
    padding: 12px 16px;
    border-right: 0;
    border-bottom: 1px solid var(--line);
  }
  .sidebar-label,
  .sidebar-empty {
    width: 100%;
    margin: 8px 4px;
  }
  .space-link {
    width: auto;
    max-width: 100%;
    padding: 10px 12px;
    margin: 0;
  }
  .sidebar-create {
    width: auto;
    margin: 0;
  }
  .home-main {
    padding: 26px 20px 60px;
  }
  .space-row {
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 14px;
  }
  .space-row--header {
    display: none;
  }
  .space-row > div:first-child {
    grid-column: 1/-1;
  }
  .space-row > div:last-child {
    grid-column: 2;
    grid-row: 2/4;
  }
  .space-tabs {
    gap: 24px;
  }
  .space-stats > div {
    padding: 22px 12px;
  }
  h1 {
    font-size: 26px;
  }
  .invite-box {
    padding: 20px;
  }
}
</style>
