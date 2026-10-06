<script setup lang="ts">
import AppLogo from '@/components/AppLogo.vue'
</script>

<template>
  <main class="auth-layout">
    <section class="auth-brand">
      <AppLogo light />
      <div class="brand-copy">
        <p class="eyebrow eyebrow--light">TWO CIRCLES, ONE SHARED SPACE</p>
        <h1>우리 사이를<br />더 가까이.</h1>
        <p>함께하는 사람들과 공간을 만들고,<br />소중한 순간을 편안하게 나눠보세요.</p>
      </div>
      <div class="people-card" aria-hidden="true">
        <div class="avatars"><span>유</span><span>민</span><span>서</span><span>+</span></div>
        <div>
          <strong>함께라서 더 좋은 공간</strong><small>SAI에서 우리만의 이야기를 시작하세요</small>
        </div>
      </div>
    </section>
    <section class="auth-panel">
      <div class="mobile-logo">
        <RouterLink to="/" aria-label="SAI 메인으로 돌아가기"><AppLogo /></RouterLink>
      </div>
      <div class="panel-content"><slot /></div>
      <p class="panel-footer">© 2026 SAI. Two circles, one shared space.</p>
    </section>
  </main>
</template>

<style scoped>
.auth-layout {
  min-height: 100svh;
  display: grid;
  grid-template-columns: minmax(25rem, 0.9fr) minmax(32rem, 1.1fr);
  background: var(--surface);
}
.auth-brand {
  position: relative;
  min-height: 100svh;
  padding: clamp(2rem, 4vw, 4.5rem);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  color: #fff;
  background:
    radial-gradient(circle at 15% 12%, rgba(124, 118, 244, 0.32), transparent 32%),
    linear-gradient(145deg, #1d1a5b, #282475 55%, #211d66);
}
.auth-brand::after {
  position: absolute;
  right: -8rem;
  bottom: -10rem;
  width: 30rem;
  height: 30rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  content: '';
}
.brand-copy {
  position: relative;
  z-index: 1;
  margin: auto 0 0;
  padding-top: 48px;
}
.mobile-logo a {
  text-decoration: none;
}
.auth-layout.auth-layout .mobile-logo {
  flex-direction: column;
  align-items: center;
}
.brand-copy h1 {
  max-width: 10ch;
  margin: 1rem 0 1.25rem;
  font-size: clamp(3.2rem, 6vw, 5.5rem);
  font-weight: 700;
  line-height: 1.04;
  letter-spacing: -0.065em;
}
.brand-copy > p:last-child {
  color: rgba(255, 255, 255, 0.7);
  font-size: 1.05rem;
  line-height: 1.8;
}
.brand-glow {
  position: absolute;
  top: 28%;
  width: 11rem;
  height: 11rem;
  border-radius: 50%;
  opacity: 0.45;
}
.brand-glow--one {
  right: -3rem;
  background: #9278ef;
}
.brand-glow--two {
  top: 34%;
  right: -8rem;
  background: #716df2;
}
.people-card {
  position: relative;
  z-index: 1;
  padding-top: 2rem;
  border-top: 1px solid rgba(255, 255, 255, 0.13);
  display: flex;
  align-items: center;
  gap: 1rem;
}
.avatars {
  display: flex;
}
.avatars span {
  width: 2.35rem;
  height: 2.35rem;
  margin-left: -0.55rem;
  border: 2px solid #25216c;
  border-radius: 50%;
  display: grid;
  place-items: center;
  color: #342d7a;
  background: #e3e0ff;
  font-size: 0.72rem;
  font-weight: 750;
}
.avatars span:first-child {
  margin-left: 0;
}
.avatars span:nth-child(2) {
  background: #efdfff;
}
.avatars span:nth-child(3) {
  background: #d7e9ff;
}
.avatars span:last-child {
  color: #fff;
  background: #6257cb;
}
.people-card strong,
.people-card small {
  display: block;
}
.people-card strong {
  margin-bottom: 0.2rem;
  font-size: 0.88rem;
}
.people-card small {
  color: rgba(255, 255, 255, 0.55);
  font-size: 0.72rem;
}
.auth-panel {
  min-height: 100svh;
  padding: 2rem clamp(2rem, 8vw, 8rem);
  display: flex;
  flex-direction: column;
}
.panel-content {
  width: 100%;
  max-width: 30rem;
  margin: auto;
}
.mobile-logo {
  display: none;
}
.panel-footer {
  margin: 2rem 0 0;
  color: var(--text-soft);
  font-size: 0.72rem;
  text-align: center;
}
@media (max-width: 850px) {
  .auth-layout {
    display: block;
  }
  .auth-brand {
    display: none;
  }
  .auth-panel {
    padding: 1.5rem;
  }
  .mobile-logo {
    display: block;
  }
  .panel-content {
    padding: 3rem 0;
  }
}
</style>
