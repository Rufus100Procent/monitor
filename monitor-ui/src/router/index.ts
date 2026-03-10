import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '../composables/useAuth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'Landing',
      meta: { guest: true },
      component: () => import('../views/LandingView.vue'),
    },
    {
      path: '/login',
      name: 'Login',
      meta: { guest: true },
      component: () => import('../views/LandingView.vue'),
    },
    {
      path: '/register',
      name: 'Register',
      meta: { guest: true },
      component: () => import('../views/LandingView.vue'),
    },
    {
      path: '/docs',
      name: 'Docs',
      meta: { fullPage: true },
      component: () => import('../views/DocsView.vue'),
    },
    {
      path: '/dashboard',
      name: 'Dashboard',
      meta: { title: 'Dashboard', requiresAuth: true },
      component: () => import('../views/DashboardView.vue'),
    },
    {
      path: '/faq',
      name: 'FAQ',
      meta: { title: 'FAQ', requiresAuth: true },
      component: () => import('../views/FaqView.vue'),
    },
    {
      path: '/about',
      redirect: '/',
    },
    {
      path: '/profile',
      name: 'Profile',
      meta: { title: 'Profile', requiresAuth: true },
      component: () => import('../views/ProfileView.vue'),
    },
    {
      path: '/server/:id',
      children: [
        {
          path: 'basics',
          meta: { title: 'Basics', requiresAuth: true },
          component: () => import('../views/server/BasicsView.vue'),
        },
        {
          path: 'track-me',
          meta: { title: 'Track Me', requiresAuth: true },
          component: () => import('../views/server/TrackMeView.vue'),
        },
        {
          path: 'settings',
          meta: { title: 'Settings', requiresAuth: true },
          component: () => import('../views/server/SettingsView.vue'),
        },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const { isAuthenticated } = useAuth()

  // Protected route, not authenticated  send to landing
  if (to.meta.requiresAuth && !isAuthenticated.value) {
    return { path: '/' }
  }

  // Already authenticated, visiting landing send to dashboard
  if (to.meta.guest && isAuthenticated.value) {
    return { path: '/dashboard' }
  }
})

export default router
