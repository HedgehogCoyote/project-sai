<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ApiError } from '@/services/api'
import { api, timeAgo, type Post } from '@/services/workspace'
import ReactionBar from './ReactionBar.vue'

const props = defineProps<{ spaceId: number; target: string; canModerate: boolean; title?: string }>()
const emit = defineEmits<{ sessionExpired: [error: ApiError]; changed: [count: number] }>()
const items = ref<Post[]>([]),
  body = ref(''),
  busy = ref(false),
  error = ref('')

function fail(e: unknown, fallback: string) {
  if (e instanceof ApiError && e.status === 401) emit('sessionExpired', e)
  error.value = e instanceof Error ? e.message : fallback
}
async function load() {
  error.value = ''
  try {
    items.value = await api.comments(props.spaceId, props.target)
  } catch (e) {
    fail(e, '댓글을 불러오지 못했습니다.')
  }
}
async function send() {
  const text = body.value.trim()
  if (!text || busy.value) return
  busy.value = true
  error.value = ''
  try {
    items.value.push(await api.postComment(props.spaceId, props.target, text))
    body.value = ''
    emit('changed', items.value.length)
  } catch (e) {
    fail(e, '댓글을 남기지 못했습니다.')
  } finally {
    busy.value = false
  }
}
async function remove(post: Post) {
  busy.value = true
  try {
    await api.deletePost(props.spaceId, post.id)
    items.value = items.value.filter((p) => p.id !== post.id)
    emit('changed', items.value.length)
  } catch (e) {
    fail(e, '댓글을 지우지 못했습니다.')
  } finally {
    busy.value = false
  }
}
onMounted(load)
watch(() => props.target, load)
</script>

<template>
  <section class="ws-comments" :aria-label="title ?? '댓글'">
    <strong v-if="title">{{ title }} <span class="ws-meta">{{ items.length }}개</span></strong>
    <p v-if="error" class="ws-error" role="alert">{{ error }}</p>
    <article v-for="c in items" :key="c.id" class="ws-comment">
      <header>
        <strong>{{ c.authorName }}</strong><span>{{ timeAgo(c.createdAt) }}</span>
        <button v-if="c.mine || canModerate" class="ws-link" type="button" :disabled="busy" @click="remove(c)">
          지우기
        </button>
      </header>
      <p class="ws-pre">{{ c.body }}</p>
      <ReactionBar :space-id="spaceId" :post="c" @updated="(p) => (items = items.map((x) => (x.id === p.id ? p : x)))" />
    </article>
    <p v-if="!items.length && !error" class="ws-meta">아직 댓글이 없습니다. 첫 댓글을 남겨 보세요.</p>
    <form class="ws-comment-form" @submit.prevent="send">
      <input v-model="body" maxlength="2000" aria-label="댓글 입력" placeholder="댓글을 남겨 주세요" :disabled="busy" />
      <button class="ws-btn primary" type="submit" :disabled="busy || !body.trim()">남기기</button>
    </form>
  </section>
</template>
