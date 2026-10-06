<script setup lang="ts">
import CircleUnion from '@/components/CircleUnion.vue'
import AppLogo from '@/components/AppLogo.vue'
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore()
</script>
<template>
  <div class="landing">
    <header class="landing-header">
      <RouterLink class="wordmark" to="/" aria-label="SAI 메인"><AppLogo /></RouterLink>
      <nav aria-label="계정">
        <RouterLink v-if="auth.user" class="account-link" to="/spaces">내 공간 ↗</RouterLink
        ><template v-else
          ><RouterLink class="login-link" to="/login">로그인</RouterLink
          ><RouterLink class="account-link" to="/signup">회원가입 ↗</RouterLink></template
        >
      </nav>
    </header>
    <main class="landing-main">
      <CircleUnion />
      <p class="kicker">A SPACE BETWEEN.</p>
      <h1>가까워지는 순간,<br />새로운 공간.</h1>
      <p class="intro">일상과 이야기가 자연스럽게 이어지는 곳, SAI.</p>
      <div class="landing-actions">
        <RouterLink class="start-link" :to="auth.user ? '/spaces' : '/signup'"
          >{{ auth.user ? '내 공간으로' : '시작하기' }}
          <span aria-hidden="true">↗</span></RouterLink
        ><RouterLink v-if="!auth.user" class="secondary-link" to="/login">로그인</RouterLink>
      </div>
    </main>
    <footer class="landing-footer">
      <span>SAI — 연결이 시작되는 곳</span><span>© 2026 SAI</span>
    </footer>
  </div>
</template>
<style scoped>
.landing {
  min-height: 100svh;
  display: flex;
  flex-direction: column;
  padding: 0 clamp(22px, 5vw, 76px);
  color: #34234f;
  background: #fcfbfe;
}
.landing-header {
  height: 84px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e9e4ef;
}
.wordmark {
  color: #34234f;
  font-size: 34px;
  font-weight: 800;
  letter-spacing: -2px;
  text-decoration: none;
}
nav {
  display: flex;
  align-items: center;
  gap: 24px;
  font-size: 13px;
}
nav a {
  text-decoration: none;
}
.login-link {
  color: #7c7189;
  padding: 10px 0;
}
.account-link {
  color: #34234f;
  padding: 10px 15px;
  border: 1px solid #e9e4ef;
  border-radius: 6px;
}
.landing-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 48px 0 60px;
  text-align: center;
}
.landing-main :deep(.union-scene) {
  max-width: 440px;
}
.kicker {
  color: #7c7189;
  font-size: 10px;
  letter-spacing: 3px;
  margin: 20px 0 18px;
}
h1 {
  margin: 0;
  font-size: clamp(34px, 4.5vw, 54px);
  line-height: 1.25;
  font-weight: 550;
  letter-spacing: -0.055em;
}
.intro {
  font-size: 14px;
  line-height: 1.9;
  margin: 18px 0 25px;
  color: #7c7189;
}
.landing-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
}
.landing-actions a {
  text-decoration: none;
  font-size: 14px;
  border-radius: 7px;
  padding: 14px 24px;
}
.start-link {
  display: flex;
  align-items: center;
  gap: 38px;
  background: #574173;
  color: #fff;
}
.start-link:hover {
  background: #34234f;
}
.secondary-link {
  color: #34234f;
  border: 1px solid #e9e4ef;
}
.secondary-link:hover,
.account-link:hover {
  background: #f0eaf8;
}
.landing-footer {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 12px;
  font-size: 11px;
  color: #7c7189;
  padding: 22px 0;
  border-top: 1px solid #e9e4ef;
}
@media (max-width: 650px) {
  .landing-header {
    height: 72px;
  }
  nav {
    gap: 12px;
  }
  .landing-main {
    padding: 36px 0 48px;
  }
  .intro {
    max-width: 30ch;
  }
}
</style>
