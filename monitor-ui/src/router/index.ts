import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/dashboard' },
    {
      path: '/dashboard',
      name: 'Dashboard',
      meta: { title: 'Dashboard' },
      component: () => import('../views/DashboardView.vue'),
    },
    {
      path: '/faq',
      name: 'FAQ',
      meta: { title: 'FAQ' },
      component: () => import('../views/FaqView.vue'),
    },
    {
      path: '/about',
      name: 'About',
      meta: { title: 'About' },
      component: () => import('../views/AboutView.vue'),
    },
    {
      path: '/profile',
      name: 'Profile',
      meta: { title: 'Profile' },
      component: () => import('../views/ProfileView.vue'),
    },
    {
      path: '/server/:id',
      children: [
        {
          path: 'basics',
          meta: { title: 'Basics' },
          component: () => import('../views/server/BasicsView.vue'),
        },
        {
          path: 'track-me',
          meta: { title: 'Track Me' },
          component: () => import('../views/server/TrackMeView.vue'),
        },
        {
          path: 'settings',
          meta: { title: 'Settings' },
          component: () => import('../views/server/SettingsView.vue'),
        },
      ],
    },
  ],
})

export default router
