<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppLogo from '@/components/AppLogo.vue'
import { ApiError } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { useSpacesStore, type SpaceRole } from '@/stores/spaces'

const auth = useAuthStore()
const spacesStore = useSpacesStore()
const router = useRouter()

const menuOpen = ref(false)
const createOpen = ref(false)
const newSpaceTitle = ref('')
const selectedSpaceId = ref<number | null>(null)
const inviteeUserId = ref<number | null>(null)
const pageError = ref('')
const createError = ref('')
const inviteError = ref('')
const inviteSuccess = ref('')

const canInvite = computed(
  () => selectedSpaceId.value !== null && inviteeUserId.value !== null && inviteeUserId.value > 0,
)

function roleLabel(role: SpaceRole) {
  return { OWNER: '소유자', MANAGER: '관리자', MEMBER: '멤버' }[role]
}

async function handleApiError(error: unknown, fallbackMessage: string) {
  if (error instanceof ApiError && error.status === 401) {
    auth.clearSession()
    spacesStore.clearSpaces()
    await router.replace({ name: 'login', query: { redirect: '/' } })
    return '로그인이 만료되었습니다. 다시 로그인해 주세요.'
  }
  return error instanceof ApiError ? error.message : fallbackMessage
}

async function loadSpaces() {
  pageError.value = ''
  try {
    await spacesStore.fetchMySpaces()
    if (selectedSpaceId.value === null && spacesStore.spaces.length > 0) {
      selectedSpaceId.value = spacesStore.spaces[0]?.spaceId ?? null
    }
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
  if (!title || title.length > 30) {
    createError.value = '공간 이름은 1자 이상 30자 이하로 입력해 주세요.'
    return
  }

  try {
    const createdSpaceId = await spacesStore.createSpace(title)
    selectedSpaceId.value = createdSpaceId
    createOpen.value = false
  } catch (error) {
    createError.value = await handleApiError(error, '공간을 만들지 못했습니다.')
  }
}

async function submitInvitation() {
  inviteError.value = ''
  inviteSuccess.value = ''
  if (!canInvite.value || selectedSpaceId.value === null || inviteeUserId.value === null) {
    inviteError.value = '공간과 초대할 사용자 ID를 확인해 주세요.'
    return
  }

  try {
    await spacesStore.inviteUser(selectedSpaceId.value, inviteeUserId.value)
    inviteSuccess.value = `${inviteeUserId.value}번 사용자에게 초대를 보냈습니다.`
    inviteeUserId.value = null
  } catch (error) {
    inviteError.value = await handleApiError(error, '초대를 보내지 못했습니다.')
  }
}

async function logout() {
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
      <RouterLink to="/" class="brand-link"><AppLogo /></RouterLink>
      <nav aria-label="주요 메뉴"><a class="active" href="#spaces">내 공간</a><a href="#invitations">사용자 초대</a></nav>
      <div class="profile">
        <button class="profile__button" type="button" :aria-expanded="menuOpen" @click="menuOpen = !menuOpen"><span>{{ auth.user?.name?.slice(0, 1) }}</span><span class="profile__copy"><strong>{{ auth.user?.name }}</strong><small>@{{ auth.user?.loginId }}</small></span><span aria-hidden="true">⌄</span></button>
        <div v-if="menuOpen" class="profile__menu"><button type="button" :disabled="auth.pending" @click="logout">로그아웃</button></div>
      </div>
    </header>

    <main class="home-main">
      <section class="welcome-section">
        <div><p class="eyebrow">MY SHARED SPACES</p><h1>{{ auth.user?.name }}님, 반가워요 <span>✦</span></h1><p>오늘도 소중한 사람들과 좋은 순간을 만들어 보세요.</p></div>
        <button class="primary-button primary-button--fit" type="button" @click="openCreateDialog"><span aria-hidden="true">＋</span> 새 공간 만들기</button>
      </section>

      <p v-if="pageError" class="status-message status-message--error" role="alert">{{ pageError }} <button type="button" @click="loadSpaces">다시 시도</button></p>
      <div v-if="spacesStore.loading" class="loading-state" aria-live="polite">공간을 불러오는 중...</div>

      <section v-else id="spaces" class="spaces-grid" aria-label="내 공간">
        <article v-for="(space, index) in spacesStore.spaces" :key="space.spaceId" class="space-card">
          <div class="space-card__art" :class="`space-card__art--${index % 3}`" aria-hidden="true"><span></span><span></span></div>
          <div class="space-card__body">
            <div><span class="space-card__badge">{{ roleLabel(space.role) }}</span><h2>{{ space.title }}</h2><p>{{ space.spaceMemberCount }}명이 함께하는 공간입니다.</p></div>
            <div class="space-card__footer"><div class="mini-avatars"><span>{{ auth.user?.name?.slice(0, 1) }}</span><span v-if="space.spaceMemberCount > 1">+{{ space.spaceMemberCount - 1 }}</span></div><strong>{{ space.spaceMemberCount }}명</strong><span class="space-id">#{{ space.spaceId }}</span></div>
          </div>
        </article>

        <div v-if="!spacesStore.hasSpaces" class="empty-state"><span>○ ○</span><h2>아직 참여 중인 공간이 없어요</h2><p>첫 공간을 만들고 소중한 사람을 초대해 보세요.</p></div>
        <button class="create-card" type="button" @click="openCreateDialog"><span class="create-card__icon">＋</span><strong>새로운 공간 만들기</strong><small>친구, 가족, 동료와 함께할<br>새로운 공간을 열어보세요.</small></button>
      </section>

      <section id="invitations" class="invitation-panel">
        <div class="invitation-panel__copy"><div class="invitation-banner__icon">♡</div><div><p class="eyebrow">INVITE A MEMBER</p><h2>공간에 사용자 초대</h2><p>현재 백엔드 API에 맞춰 사용자 ID로 초대장을 보냅니다.</p></div></div>
        <form class="invitation-form" @submit.prevent="submitInvitation">
          <label>초대할 공간<select v-model="selectedSpaceId" required :disabled="!spacesStore.hasSpaces"><option :value="null" disabled>공간 선택</option><option v-for="space in spacesStore.spaces" :key="space.spaceId" :value="space.spaceId">{{ space.title }}</option></select></label>
          <label>사용자 ID<input v-model.number="inviteeUserId" type="number" min="1" step="1" placeholder="예: 12" required></label>
          <button type="submit" :disabled="!canInvite || spacesStore.inviting">{{ spacesStore.inviting ? '초대 중...' : '초대 보내기' }}</button>
        </form>
        <p v-if="inviteError" class="status-message status-message--error" role="alert">{{ inviteError }}</p>
        <p v-if="inviteSuccess" class="status-message status-message--success" role="status">{{ inviteSuccess }}</p>
      </section>
    </main>

    <div v-if="createOpen" class="modal-backdrop" @click.self="closeCreateDialog">
      <section class="modal" role="dialog" aria-modal="true" aria-labelledby="create-space-title">
        <button class="modal__close" type="button" aria-label="닫기" :disabled="spacesStore.creating" @click="closeCreateDialog">×</button>
        <p class="eyebrow">NEW SHARED SPACE</p><h2 id="create-space-title">새 공간 만들기</h2><p>함께할 공간의 이름을 정해 주세요.</p>
        <form @submit.prevent="submitCreate">
          <div class="field"><label for="spaceTitle">공간 이름</label><input id="spaceTitle" v-model="newSpaceTitle" maxlength="30" autofocus placeholder="예: 우리 가족 이야기" required><small>{{ newSpaceTitle.trim().length }}/30</small></div>
          <p v-if="createError" class="form-error" role="alert">{{ createError }}</p>
          <div class="modal__actions"><button type="button" :disabled="spacesStore.creating" @click="closeCreateDialog">취소</button><button class="primary-button" type="submit" :disabled="spacesStore.creating">{{ spacesStore.creating ? '만드는 중...' : '공간 만들기' }}</button></div>
        </form>
      </section>
    </div>
  </div>
</template>

<style scoped>
.home-shell{min-height:100svh;background:#f8f8fd}.site-header{height:5.5rem;padding:0 clamp(1.5rem,6vw,6rem);border-bottom:1px solid var(--line);display:flex;align-items:center;gap:clamp(2rem,6vw,6rem);background:rgba(255,255,255,.9);backdrop-filter:blur(16px)}.brand-link{text-decoration:none}.site-header nav{height:100%;display:flex;gap:2rem}.site-header nav a{position:relative;display:grid;place-items:center;color:var(--text-muted);font-size:.9rem;font-weight:650;text-decoration:none}.site-header nav a.active{color:var(--navy-900)}.site-header nav a.active::after{position:absolute;right:0;bottom:0;left:0;height:2px;background:var(--violet-600);content:''}
.profile{position:relative;margin-left:auto}.profile__button{padding:.4rem;border:0;display:flex;align-items:center;gap:.65rem;color:var(--text);background:transparent;cursor:pointer}.profile__button>span:first-child{width:2.4rem;height:2.4rem;border-radius:50%;display:grid;place-items:center;color:#fff;background:linear-gradient(135deg,#7776f4,#9a68df);font-weight:750}.profile__copy{display:flex;flex-direction:column;align-items:flex-start}.profile__copy strong{font-size:.82rem}.profile__copy small{color:var(--text-soft)}.profile__menu{position:absolute;top:calc(100% + .5rem);right:0;z-index:10;min-width:9rem;padding:.45rem;border:1px solid var(--line);border-radius:.75rem;background:#fff;box-shadow:var(--shadow)}.profile__menu button{width:100%;padding:.65rem .8rem;border:0;border-radius:.5rem;color:#cf3e55;background:transparent;text-align:left;cursor:pointer}
.home-main{width:min(72rem,calc(100% - 3rem));margin:0 auto;padding:clamp(3rem,6vw,5rem) 0}.welcome-section{margin-bottom:2.5rem;display:flex;align-items:flex-end;justify-content:space-between;gap:2rem}.welcome-section h1{margin:.65rem 0 .55rem;font-size:clamp(2rem,4vw,3.25rem);letter-spacing:-.055em}.welcome-section h1 span{color:#756bea;font-size:.6em}.welcome-section>div>p:last-child{color:var(--text-muted)}.primary-button--fit{width:auto;flex:0 0 auto}.loading-state,.empty-state{min-height:15rem;border:1px solid var(--line);border-radius:1.5rem;display:grid;place-content:center;text-align:center;background:#fff;color:var(--text-muted)}.empty-state span{color:var(--violet-600);font-size:2rem}.empty-state h2{margin:.75rem 0 .25rem;color:var(--text)}.empty-state p{margin:0}
.spaces-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:1.25rem}.space-card{min-height:18rem;border:1px solid var(--line);border-radius:1.5rem;overflow:hidden;display:grid;grid-template-columns:.78fr 1.22fr;background:#fff;box-shadow:0 10px 35px rgba(39,32,103,.07)}.space-card__art{position:relative;overflow:hidden;background:linear-gradient(145deg,#26226f,#39318e)}.space-card__art--1{background:linear-gradient(145deg,#5e315f,#8e4d83)}.space-card__art--2{background:linear-gradient(145deg,#24505e,#397a85)}.space-card__art span{position:absolute;top:50%;width:7rem;height:7rem;border-radius:50%;transform:translateY(-50%)}.space-card__art span:first-child{left:10%;background:linear-gradient(135deg,#7776f4,#aaa5ff)}.space-card__art span:last-child{right:4%;background:linear-gradient(135deg,#7654d4,#ad78e5);opacity:.9}.space-card__body{padding:1.75rem;display:flex;flex-direction:column;justify-content:space-between}.space-card__badge{color:var(--violet-600);font-size:.66rem;font-weight:800;letter-spacing:.08em}.space-card h2{margin:.8rem 0 .65rem;font-size:1.4rem;letter-spacing:-.04em}.space-card p{color:var(--text-muted);font-size:.9rem;line-height:1.6}.space-card__footer{padding-top:1.25rem;border-top:1px solid var(--line);display:flex;align-items:center;gap:.5rem}.space-card__footer strong,.space-id{color:var(--text-muted);font-size:.75rem}.space-id{margin-left:auto}.mini-avatars{display:flex}.mini-avatars span{min-width:1.8rem;height:1.8rem;padding:0 .35rem;border:2px solid #fff;border-radius:999px;display:grid;place-items:center;color:#fff;background:#7770e8;font-size:.65rem}.mini-avatars span+span{margin-left:-.45rem;background:#c7c2eb}
.create-card{min-height:18rem;border:1px dashed #c8c4df;border-radius:1.5rem;display:flex;flex-direction:column;align-items:center;justify-content:center;color:var(--text);background:rgba(255,255,255,.62);cursor:pointer;transition:.2s ease}.create-card:hover{border-color:#8278e9;transform:translateY(-2px);background:#fff}.create-card__icon{width:3.4rem;height:3.4rem;margin-bottom:1rem;border-radius:50%;display:grid;place-items:center;color:#fff;background:linear-gradient(135deg,#706af0,#9b66dc);font-size:1.7rem}.create-card strong{font-size:1rem}.create-card small{margin-top:.55rem;color:var(--text-soft);line-height:1.6;text-align:center}
.invitation-panel{margin-top:1.25rem;padding:1.5rem 1.75rem;border:1px solid var(--line);border-radius:1.25rem;background:#fff}.invitation-panel__copy{display:flex;align-items:center;gap:1.25rem}.invitation-banner__icon{width:3.5rem;height:3.5rem;border-radius:1rem;display:grid;place-items:center;color:#6e62dd;background:#eeecff;font-size:1.5rem}.invitation-panel h2{margin:.2rem 0;font-size:1rem}.invitation-panel__copy p:last-child{margin:0;color:var(--text-muted);font-size:.82rem}.invitation-form{margin-top:1.25rem;display:grid;grid-template-columns:1.2fr 1fr auto;align-items:end;gap:.75rem}.invitation-form label{display:grid;gap:.45rem;color:#403e51;font-size:.78rem;font-weight:700}.invitation-form select,.invitation-form input{height:2.8rem;padding:0 .8rem;border:1px solid #dfdee8;border-radius:.65rem;background:#fbfbfd}.invitation-form button,.status-message button{height:2.8rem;padding:0 1rem;border:0;border-radius:.65rem;color:#fff;background:var(--navy-900);font-weight:700;cursor:pointer}.invitation-form button:disabled{opacity:.5;cursor:not-allowed}.status-message{margin:1rem 0 0;padding:.8rem 1rem;border-radius:.65rem;font-size:.82rem}.status-message--error{color:var(--danger);background:#fff0f3}.status-message--success{color:#28704d;background:#eefaf4}.status-message button{height:auto;margin-left:.5rem;padding:.35rem .65rem}
.modal-backdrop{position:fixed;inset:0;z-index:50;padding:1rem;display:grid;place-items:center;background:rgba(24,21,67,.5);backdrop-filter:blur(4px)}.modal{position:relative;width:min(28rem,100%);padding:2rem;border-radius:1.25rem;background:#fff;box-shadow:var(--shadow)}.modal h2{margin:.6rem 0;font-size:1.8rem}.modal>p:not(.eyebrow){margin:0 0 1.5rem;color:var(--text-muted)}.modal__close{position:absolute;top:1rem;right:1rem;width:2rem;height:2rem;border:0;border-radius:50%;background:#f1f0f7;color:var(--text-muted);font-size:1.25rem;cursor:pointer}.modal .field{position:relative}.modal .field small{position:absolute;right:0;top:0;color:var(--text-soft)}.modal__actions{margin-top:1.5rem;display:grid;grid-template-columns:1fr 1.5fr;gap:.75rem}.modal__actions>button:first-child{border:1px solid var(--line);border-radius:.8rem;background:#fff;font-weight:700;cursor:pointer}
@media(max-width:900px){.spaces-grid{grid-template-columns:1fr}.invitation-form{grid-template-columns:1fr 1fr}.invitation-form button{grid-column:1/-1}}@media(max-width:820px){.site-header nav{display:none}.profile__copy{display:none}.welcome-section{align-items:flex-start;flex-direction:column}.space-card{grid-template-columns:1fr}.space-card__art{min-height:10rem}.home-main{width:min(100% - 2rem,40rem)}}@media(max-width:560px){.invitation-form{grid-template-columns:1fr}.invitation-form button{grid-column:auto}.invitation-panel__copy{align-items:flex-start}.modal__actions{grid-template-columns:1fr}}
</style>
