import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: () => import('@/views/HomeView.vue'), meta: { auth: true } },
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { guest: true } },
    { path: '/signup', name: 'signup', component: () => import('@/views/SignupView.vue'), meta: { guest: true } },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!auth.initialized) await auth.fetchMe()
  if (to.meta.auth && !auth.user) return { name: 'login', query: { redirect: to.fullPath } }
  if (to.meta.guest && auth.user) return { name: 'home' }
})

export default router
