<script setup lang="ts">
import { ref, useId } from 'vue'
const filterId = `union-${useId().replace(/:/g, '')}`
const playing = ref(true)
const iteration = ref(0)
function replay() {
  iteration.value += 1
  playing.value = true
}
</script>
<template>
  <div class="union-scene" :class="{ 'is-paused': !playing }">
    <svg class="filter-definitions" width="0" height="0" aria-hidden="true">
      <defs>
        <filter
          :id="filterId"
          x="-35%"
          y="-45%"
          width="170%"
          height="190%"
          color-interpolation-filters="sRGB"
        >
          <feGaussianBlur in="SourceGraphic" stdDeviation="13" result="soft" />
          <feColorMatrix
            in="soft"
            type="matrix"
            values="1 0 0 0 0  0 1 0 0 0  0 0 1 0 0  0 0 0 18 -7"
          />
        </filter>
      </defs>
    </svg>
    <div
      :key="iteration"
      class="union-art"
      role="img"
      aria-label="두 원이 닿기 전에 물방울처럼 이어져 하나의 그라데이션 형태가 됩니다"
    >
      <div class="union-orbit" aria-hidden="true"></div>
      <div class="union-liquid" :style="{ filter: `url(#${filterId})` }" aria-hidden="true">
        <div class="union-circle union-circle--left"></div>
        <div class="union-circle union-circle--right"></div>
      </div>
    </div>
    <div class="union-controls">
      <button
        type="button"
        :aria-label="playing ? '원 애니메이션 일시정지' : '원 애니메이션 재생'"
        @click="playing = !playing"
      >
        {{ playing ? '일시정지' : '재생' }}</button
      ><button type="button" aria-label="원 애니메이션 다시 보기" @click="replay">
        움직임 다시 보기
      </button>
    </div>
  </div>
</template>
<style scoped>
.union-scene {
  width: 100%;
  color: #7c7189;
}
.filter-definitions {
  position: absolute;
  pointer-events: none;
}
.union-art {
  position: relative;
  width: min(440px, 100%);
  height: 228px;
  margin: auto;
  isolation: isolate;
}
.union-liquid {
  position: absolute;
  inset: 0;
}
.union-circle {
  position: absolute;
  top: 10px;
  width: 190px;
  height: 190px;
  border-radius: 50%;
  animation: merge 7s cubic-bezier(0.42, 0, 0.25, 1) both;
}
.union-circle--left {
  left: calc(50% - 175px);
  --start: -35px;
  --end: 26px;
  background: radial-gradient(
    ellipse at 23% 18%,
    #fff0e6 0%,
    #ffc3e4 24%,
    #ed8cd8 43%,
    #b983f2 68%,
    #8165eb 100%
  );
}
.union-circle--right {
  right: calc(50% - 175px);
  --start: 35px;
  --end: -26px;
  background: radial-gradient(
    ellipse at 75% 22%,
    #d9fff7 0%,
    #97e4f2 24%,
    #8cbbf8 43%,
    #a38df0 68%,
    #8165eb 100%
  );
}
.union-orbit {
  position: absolute;
  width: 240px;
  height: 240px;
  top: -16px;
  left: calc(50% - 120px);
  border: 1px solid #e9e4ef;
  border-radius: 50%;
}
.union-controls {
  position: relative;
  display: flex;
  justify-content: center;
  gap: 16px;
}
.union-controls button {
  border: 0;
  padding: 8px 2px;
  background: none;
  color: inherit;
  font-size: 11px;
  cursor: pointer;
}
.union-controls button:hover {
  color: #574173;
}
.is-paused .union-circle {
  animation-play-state: paused;
}
@keyframes merge {
  from {
    transform: translateX(var(--start));
  }
  to {
    transform: translateX(var(--end));
  }
}
@media (max-width: 650px) {
  .union-circle {
    width: 150px;
    height: 150px;
    top: 30px;
  }
  .union-circle--left {
    left: calc(50% - 145px);
  }
  .union-circle--right {
    right: calc(50% - 145px);
  }
}
@media (prefers-reduced-motion: reduce) {
  .union-circle {
    animation: none;
    transform: translateX(var(--end));
  }
  .union-controls {
    display: none;
  }
}
</style>
