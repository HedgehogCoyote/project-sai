<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AppLogo from '@/components/AppLogo.vue'
import { useAuthStore } from '@/stores/auth'

const auth=useAuthStore(); const router=useRouter(); const menuOpen=ref(false)
async function logout(){ await auth.logout(); await router.replace('/login') }
</script>

<template>
  <div class="home-shell">
    <header class="site-header">
      <RouterLink to="/" class="brand-link"><AppLogo /></RouterLink>
      <nav aria-label="주요 메뉴"><a class="active" href="#spaces">내 공간</a><a href="#invitations">초대</a></nav>
      <div class="profile">
        <button class="profile__button" type="button" @click="menuOpen=!menuOpen"><span>{{ auth.user?.name?.slice(0,1) }}</span><span class="profile__copy"><strong>{{ auth.user?.name }}</strong><small>@{{ auth.user?.loginId }}</small></span><span aria-hidden="true">⌄</span></button>
        <div v-if="menuOpen" class="profile__menu"><button type="button" :disabled="auth.pending" @click="logout">로그아웃</button></div>
      </div>
    </header>

    <main class="home-main">
      <section class="welcome-section">
        <div><p class="eyebrow">MY SHARED SPACES</p><h1>{{ auth.user?.name }}님, 반가워요 <span>✦</span></h1><p>오늘도 소중한 사람들과 좋은 순간을 만들어 보세요.</p></div>
        <button class="primary-button primary-button--fit" type="button"><span aria-hidden="true">＋</span> 새 공간 만들기</button>
      </section>

      <section id="spaces" class="spaces-grid" aria-label="내 공간">
        <article class="space-card">
          <div class="space-card__art" aria-hidden="true"><span></span><span></span></div>
          <div class="space-card__body">
            <div><span class="space-card__badge">MY FIRST SPACE</span><h2>우리의 첫 번째 공간</h2><p>함께 나누고 싶은 이야기를 시작해 보세요.</p></div>
            <div class="space-card__footer"><div class="mini-avatars"><span>{{ auth.user?.name?.slice(0,1) }}</span><span>+</span></div><strong>1명</strong><button type="button">공간 열기 →</button></div>
          </div>
        </article>
        <button class="create-card" type="button"><span class="create-card__icon">＋</span><strong>새로운 공간 만들기</strong><small>친구, 가족, 동료와 함께할<br>새로운 공간을 열어보세요.</small></button>
      </section>

      <section id="invitations" class="invitation-banner">
        <div class="invitation-banner__icon">♡</div><div><p class="eyebrow">INVITATIONS</p><h2>도착한 초대가 없어요</h2><p>누군가 공간에 초대하면 이곳에서 확인할 수 있어요.</p></div><button type="button">초대 코드 입력</button>
      </section>
    </main>
  </div>
</template>

<style scoped>
.home-shell{min-height:100svh;background:#f8f8fd}.site-header{height:5.5rem;padding:0 clamp(1.5rem,6vw,6rem);border-bottom:1px solid var(--line);display:flex;align-items:center;gap:clamp(2rem,6vw,6rem);background:rgba(255,255,255,.9);backdrop-filter:blur(16px)}.brand-link{text-decoration:none}.site-header nav{height:100%;display:flex;gap:2rem}.site-header nav a{position:relative;display:grid;place-items:center;color:var(--text-muted);font-size:.9rem;font-weight:650;text-decoration:none}.site-header nav a.active{color:var(--navy-900)}.site-header nav a.active::after{position:absolute;right:0;bottom:0;left:0;height:2px;background:var(--violet-600);content:''}
.profile{position:relative;margin-left:auto}.profile__button{padding:.4rem;border:0;display:flex;align-items:center;gap:.65rem;color:var(--text);background:transparent;cursor:pointer}.profile__button>span:first-child{width:2.4rem;height:2.4rem;border-radius:50%;display:grid;place-items:center;color:#fff;background:linear-gradient(135deg,#7776f4,#9a68df);font-weight:750}.profile__copy{display:flex;flex-direction:column;align-items:flex-start}.profile__copy strong{font-size:.82rem}.profile__copy small{color:var(--text-soft)}.profile__menu{position:absolute;top:calc(100% + .5rem);right:0;z-index:10;min-width:9rem;padding:.45rem;border:1px solid var(--line);border-radius:.75rem;background:#fff;box-shadow:var(--shadow)}.profile__menu button{width:100%;padding:.65rem .8rem;border:0;border-radius:.5rem;color:#cf3e55;background:transparent;text-align:left;cursor:pointer}
.home-main{width:min(72rem,calc(100% - 3rem));margin:0 auto;padding:clamp(3rem,6vw,5rem) 0}.welcome-section{margin-bottom:2.5rem;display:flex;align-items:flex-end;justify-content:space-between;gap:2rem}.welcome-section h1{margin:.65rem 0 .55rem;font-size:clamp(2rem,4vw,3.25rem);letter-spacing:-.055em}.welcome-section h1 span{color:#756bea;font-size:.6em}.welcome-section>div>p:last-child{color:var(--text-muted)}.primary-button--fit{width:auto;flex:0 0 auto}
.spaces-grid{display:grid;grid-template-columns:1.7fr 1fr;gap:1.25rem}.space-card{min-height:20rem;border:1px solid var(--line);border-radius:1.5rem;overflow:hidden;display:grid;grid-template-columns:.85fr 1.15fr;background:#fff;box-shadow:0 10px 35px rgba(39,32,103,.07)}.space-card__art{position:relative;overflow:hidden;background:linear-gradient(145deg,#26226f,#39318e)}.space-card__art span{position:absolute;top:50%;width:9rem;height:9rem;border-radius:50%;transform:translateY(-50%)}.space-card__art span:first-child{left:12%;background:linear-gradient(135deg,#7776f4,#aaa5ff)}.space-card__art span:last-child{right:7%;background:linear-gradient(135deg,#7654d4,#ad78e5);opacity:.9}.space-card__body{padding:2rem;display:flex;flex-direction:column;justify-content:space-between}.space-card__badge{color:var(--violet-600);font-size:.66rem;font-weight:800;letter-spacing:.15em}.space-card h2{margin:.8rem 0 .65rem;font-size:1.5rem;letter-spacing:-.04em}.space-card p{color:var(--text-muted);font-size:.9rem;line-height:1.6}.space-card__footer{padding-top:1.25rem;border-top:1px solid var(--line);display:flex;align-items:center;gap:.5rem}.space-card__footer strong{color:var(--text-muted);font-size:.75rem}.space-card__footer button{margin-left:auto;border:0;color:var(--navy-900);background:transparent;font-weight:750;cursor:pointer}.mini-avatars{display:flex}.mini-avatars span{width:1.8rem;height:1.8rem;border:2px solid #fff;border-radius:50%;display:grid;place-items:center;color:#fff;background:#7770e8;font-size:.65rem}.mini-avatars span+span{margin-left:-.45rem;background:#c7c2eb}
.create-card{min-height:20rem;border:1px dashed #c8c4df;border-radius:1.5rem;display:flex;flex-direction:column;align-items:center;justify-content:center;color:var(--text);background:rgba(255,255,255,.62);cursor:pointer;transition:.2s ease}.create-card:hover{border-color:#8278e9;transform:translateY(-2px);background:#fff}.create-card__icon{width:3.4rem;height:3.4rem;margin-bottom:1rem;border-radius:50%;display:grid;place-items:center;color:#fff;background:linear-gradient(135deg,#706af0,#9b66dc);font-size:1.7rem}.create-card strong{font-size:1rem}.create-card small{margin-top:.55rem;color:var(--text-soft);line-height:1.6}
.invitation-banner{margin-top:1.25rem;padding:1.5rem 1.75rem;border:1px solid var(--line);border-radius:1.25rem;display:flex;align-items:center;gap:1.25rem;background:#fff}.invitation-banner__icon{width:3.5rem;height:3.5rem;border-radius:1rem;display:grid;place-items:center;color:#6e62dd;background:#eeecff;font-size:1.5rem}.invitation-banner h2{margin:.2rem 0;font-size:1rem}.invitation-banner p:last-child{color:var(--text-muted);font-size:.82rem}.invitation-banner button{margin-left:auto;padding:.7rem 1rem;border:1px solid var(--line);border-radius:.65rem;color:var(--navy-900);background:#fff;font-weight:700;cursor:pointer}
@media(max-width:820px){.site-header nav{display:none}.profile__copy{display:none}.welcome-section{align-items:flex-start;flex-direction:column}.spaces-grid{grid-template-columns:1fr}.space-card{grid-template-columns:1fr}.space-card__art{min-height:12rem}.invitation-banner{align-items:flex-start;flex-wrap:wrap}.invitation-banner button{width:100%;margin-left:0}.home-main{width:min(100% - 2rem,40rem)}}
</style>
