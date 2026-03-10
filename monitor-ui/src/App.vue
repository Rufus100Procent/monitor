<script setup lang="ts">
import { useRoute, RouterView } from 'vue-router'
import AppSidebar from './components/AppSidebar.vue'
import AppTopBar from './components/AppTopBar.vue'
import { useSidebar } from './composables/useSidebar'
import { useAuth } from './composables/useAuth'

const route = useRoute()
const { open: sidebarOpen, close: closeSidebar } = useSidebar()
const { isAuthenticated } = useAuth()
</script>

<template>
  <!-- Unauthenticated or full-page routes (docs, landing, auth) -->
  <RouterView v-if="!isAuthenticated || route.meta.fullPage" />

  <!-- Authenticated: full app layout -->
  <div v-else class="layout">
    <div class="backdrop" :class="{ visible: sidebarOpen }" @click="closeSidebar" />
    <AppSidebar />

    <div class="main-area">
      <AppTopBar />

      <div class="content-area">
        <div class="content-topbar">
          <span class="page-title">{{ (route.meta.title as string) || route.name }}</span>
        </div>

        <main class="content-body">
          <RouterView />
        </main>
      </div>
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  height: 100%;
  overflow: hidden;
}

.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--bg);
}

.content-topbar {
  height: 44px;
  display: flex;
  align-items: center;
  padding: 0 24px;
  border-bottom: 1px solid var(--border);
  background: var(--surface);
  flex-shrink: 0;
}

.page-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: capitalize;
}

.content-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.backdrop {
  position: fixed;
  inset: 0;
  background: hsl(220, 20%, 5%, 0.45);
  z-index: 199;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.25s;
}

@media (max-width: 768px) {
  .backdrop.visible {
    opacity: 1;
    pointer-events: auto;
  }
}
</style>
