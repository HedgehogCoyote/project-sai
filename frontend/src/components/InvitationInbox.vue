<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate } from 'vue-router'
import { useInvitationsStore, type ReceivedInvitation } from '@/stores/invitations'
const inbox = useInvitationsStore()
const emit = defineEmits<{ navigate: [spaceId?: number] }>()
const tab = ref<'pending' | 'history'>('pending')
const denying = ref<ReceivedInvitation | null>(null)
const dialog = ref<HTMLDialogElement | null>(null)
const historyCount = computed(
  () => inbox.items.filter((i) => i.invitationStatus !== 'PENDING').length,
)
const visible = computed(() =>
  inbox.items.filter((i) => (i.invitationStatus === 'PENDING') === (tab.value === 'pending')),
)
const locked = computed(
  () => inbox.busy || inbox.unresolved || !inbox.confirmed || !inbox.membershipConfirmed,
)
const canLeave = () => inbox.processing === null
onBeforeRouteLeave(canLeave)
onBeforeRouteUpdate(canLeave)
function beforeUnload(event: BeforeUnloadEvent) {
  if (!canLeave()) {
    event.preventDefault()
    event.returnValue = ''
  }
}
onMounted(() => window.addEventListener('beforeunload', beforeUnload))
onUnmounted(() => window.removeEventListener('beforeunload', beforeUnload))
async function confirmDeny(item: ReceivedInvitation) {
  denying.value = item
  await nextTick()
  dialog.value?.showModal()
}
function closeDeny() {
  dialog.value?.close()
  denying.value = null
}
function deny() {
  const item = denying.value
  closeDeny()
  if (item) void inbox.process(item, 'deny')
}
</script>
<template>
  <section class="inbox" aria-labelledby="inbox-title">
    <div class="inbox-heading">
      <div>
        <h1 id="inbox-title">받은 초대</h1>
        <p>함께할 공간을 확인하고, 참여 여부를 선택하세요.</p>
      </div>
      <button :disabled="inbox.processing !== null" @click="emit('navigate')">내 공간으로</button>
    </div>
    <div v-if="inbox.notice" class="inbox-notice" role="status">
      {{ inbox.notice }}
      <button
        v-if="inbox.acceptedSpaceId && inbox.isMember(inbox.acceptedSpaceId)"
        :disabled="inbox.busy"
        @click="emit('navigate', inbox.acceptedSpaceId)"
      >
        공간으로 이동
      </button>
    </div>
    <div class="inbox-tabs" role="tablist" aria-label="초대 상태">
      <button
        role="tab"
        :aria-selected="tab === 'pending'"
        :disabled="inbox.processing !== null"
        @click="tab = 'pending'"
      >
        대기 중 <span v-if="inbox.pendingCount !== null">{{ inbox.pendingCount }}</span>
      </button>
      <button
        role="tab"
        :aria-selected="tab === 'history'"
        :disabled="inbox.processing !== null"
        @click="tab = 'history'"
      >
        처리 완료 <span v-if="inbox.confirmed">{{ historyCount }}</span>
      </button>
    </div>
    <div class="inbox-tools">
      <span>참여 여부를 선택해 주세요.</span
      ><button :disabled="inbox.busy" @click="inbox.refresh">
        {{ inbox.unresolved ? '초대 상태 확인' : '새로고침' }}
      </button>
    </div>
    <p v-if="inbox.error" class="inbox-error" role="alert">{{ inbox.error }}</p>
    <p v-if="inbox.loading" role="status" class="inbox-empty">초대를 불러오는 중...</p>
    <template v-else-if="inbox.confirmed">
      <p v-if="!visible.length" class="inbox-empty">
        {{ tab === 'pending' ? '아직 받은 초대가 없어요.' : '처리 완료된 초대가 없어요.' }}
      </p>
      <article
        v-for="(item, index) in visible"
        :key="item.invitationId"
        class="inbox-card"
        :aria-label="item.spaceName"
      >
        <div class="circle-cover" :class="{ green: index % 2 }" aria-hidden="true">
          <i></i><i></i>
        </div>
        <div class="inbox-copy">
          <h2>
            {{ item.spaceName }}
            <small>{{
              { PENDING: '대기 중', ACCEPTED: '수락 완료', DENIED: '거절 완료' }[
                item.invitationStatus
              ]
            }}</small>
          </h2>
          <p>{{ item.inviterName }} 님이 초대했어요.</p>
          <p>
            {{
              inbox.isMember(item.spaceId)
                ? '이미 참여 중인 공간이에요.'
                : item.invitationStatus === 'PENDING'
                  ? '수락하면 이 공간에 멤버로 참여해요.'
                  : '처리된 초대입니다.'
            }}
          </p>
        </div>
        <div class="inbox-actions">
          <template v-if="item.invitationStatus === 'PENDING'">
            <button :disabled="locked" @click="confirmDeny(item)">거절</button>
            <button
              v-if="!inbox.isMember(item.spaceId)"
              class="accept"
              :disabled="locked"
              @click="inbox.process(item, 'join')"
            >
              {{ inbox.processing === item.invitationId ? '처리 중...' : '수락' }}
            </button>
          </template>
          <button
            v-if="inbox.isMember(item.spaceId) && item.invitationStatus !== 'DENIED'"
            class="accept"
            :disabled="inbox.busy"
            @click="emit('navigate', item.spaceId)"
          >
            공간으로 이동
          </button>
        </div>
      </article>
    </template>
    <p class="inbox-footnote">
      수락하면 공간의 가구 배치를 함께 볼 수 있어요.<br />활동·도구의 내용은 아직 각 브라우저에
      저장됩니다.
    </p>
    <dialog
      v-if="denying"
      ref="dialog"
      class="deny-dialog"
      aria-labelledby="deny-title"
      @cancel.prevent="closeDeny"
      @click.self="closeDeny"
    >
      <h2 id="deny-title">초대를 거절할까요?</h2>
      <p>
        <strong>{{ denying.spaceName }}</strong
        ><br />{{ denying.inviterName }} 님이 보낸 초대
      </p>
      <p>이번 초대는 거절 처리돼요. 나중에 참여하려면 새 초대가 필요해요.</p>
      <div class="inbox-actions">
        <button autofocus @click="closeDeny">돌아가기</button
        ><button class="accept" @click="deny">초대 거절</button>
      </div>
    </dialog>
  </section>
</template>
<style scoped>
.inbox {
  max-width: 1030px;
  margin: auto;
  color: #fff5df;
}
.inbox-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}
.inbox h1 {
  font-size: 30px;
  margin: 0;
}
.inbox-heading p,
.inbox-copy p {
  font-size: 13px;
  line-height: 1.7;
}
.inbox button {
  border: 1px solid #decfca;
  border-radius: 9px;
  padding: 12px 18px;
  background: transparent;
  color: inherit;
  cursor: pointer;
  font-size: 13px;
}
.inbox button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.inbox-tabs {
  display: flex;
  gap: 22px;
  border-bottom: 1px solid #ffffff40;
  margin: 30px 0 22px;
}
.inbox-tabs button {
  border: 0;
  border-bottom: 3px solid transparent;
  border-radius: 0;
  padding: 16px 2px;
}
.inbox-tabs button[aria-selected='true'] {
  border-color: #fff0bf;
  font-weight: 700;
}
.inbox-tools {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  margin-bottom: 12px;
}
.inbox-tools button {
  border: 0;
}
.inbox-card {
  display: flex;
  gap: 22px;
  align-items: center;
  background: #fff8eb;
  color: #665169;
  padding: 28px;
  border-radius: 16px;
  margin-bottom: 15px;
}
.circle-cover {
  width: 78px;
  height: 78px;
  background: #e9d6e3;
  border-radius: 15px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.circle-cover i {
  height: 34px;
  width: 34px;
  border-radius: 50%;
  background: #b597b3;
}
.circle-cover i + i {
  margin-left: -18px;
  background: #f4e6ce;
  opacity: 0.92;
}
.circle-cover.green {
  background: #dfe8d7;
}
.circle-cover.green i:first-child {
  background: #a4b393;
}
.inbox-copy {
  min-width: 0;
  flex: 1;
  overflow-wrap: anywhere;
}
.inbox-copy h2 {
  font-size: 18px;
  margin: 0 0 8px;
}
.inbox-copy small {
  display: inline-block;
  font-size: 11px;
  font-weight: 400;
  border: 1px solid #cdbaca;
  border-radius: 20px;
  padding: 3px 9px;
  margin-left: 5px;
}
.inbox-copy p {
  margin: 4px 0;
}
.inbox-actions {
  display: flex;
  gap: 9px;
  flex-shrink: 0;
}
.inbox-actions button {
  min-width: 85px;
}
.inbox button.accept {
  background: #70566e;
  color: #fff8eb;
  border-color: #70566e;
}
.inbox-footnote {
  font-size: 12px;
  line-height: 1.7;
  margin: 30px 0;
  border-bottom: 1px solid #ffffff30;
  padding-bottom: 30px;
}
.inbox-error,
.inbox-notice {
  padding: 16px;
  border-radius: 9px;
  margin-top: 20px;
  background: #fff1ed;
  color: #803d45;
  font-size: 14px;
  overflow-wrap: anywhere;
}
.inbox-notice {
  background: #eef5e8;
  color: #435f43;
}
.inbox-notice button {
  margin-left: 10px;
}
.inbox-empty {
  padding: 45px 20px;
  text-align: center;
  border: 1px dashed #ffffff50;
  border-radius: 12px;
}
.deny-dialog {
  width: min(460px, calc(100% - 32px));
  box-sizing: border-box;
  border: 0;
  border-radius: 18px;
  padding: 30px;
  background: #fff8eb;
  color: #665169;
}
.deny-dialog::backdrop {
  background: #30263780;
}
.deny-dialog p {
  line-height: 1.8;
  font-size: 14px;
}
.deny-dialog .inbox-actions {
  justify-content: flex-end;
}
.deny-dialog h2 {
  font-size: 22px;
}
@media (max-width: 700px) {
  .inbox-heading {
    align-items: flex-start;
  }
  .inbox-heading button {
    flex-shrink: 0;
    padding: 10px;
  }
  .inbox h1 {
    font-size: 26px;
  }
  .inbox-card {
    padding: 20px;
    gap: 15px;
    flex-wrap: wrap;
  }
  .circle-cover {
    width: 58px;
    height: 58px;
  }
  .inbox-copy h2 {
    font-size: 16px;
  }
  .inbox-actions {
    width: 100%;
    justify-content: flex-end;
  }
  .inbox-actions button {
    flex: 1;
  }
  .inbox-notice button {
    display: block;
    margin: 10px 0 0;
  }
}
</style>
