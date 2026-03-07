<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, RouterView } from 'vue-router'
import AppSidebar from './components/AppSidebar.vue'
import AppTopBar from './components/AppTopBar.vue'
import { LoaderCircle } from 'lucide-vue-next'
import { useSidebar } from './composables/useSidebar'

const route = useRoute()
const loading = ref(false)
const { open: sidebarOpen, close: closeSidebar } = useSidebar()
</script>

<template>
  <div class="layout">
    <div class="backdrop" :class="{ visible: sidebarOpen }" @click="closeSidebar" />
    <AppSidebar />

    <div class="main-area">
      <AppTopBar />

      <div class="content-area">
        <div class="content-topbar">
          <span class="page-title">{{ route.name }}</span>
        </div>

        <main class="content-body">
          <RouterView v-slot="{ Component }">
            <Suspense @pending="loading = true" @resolve="loading = false" @fallback="loading = true">
              <component :is="Component" />
              <template #fallback>
                <div class="loader">
                  <LoaderCircle :size="26" class="spin" />
                  <span>Loading…</span>
                </div>
              </template>
            </Suspense>
          </RouterView>
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

.loader {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  height: 200px;
  color: var(--text-muted);
  font-size: 13px;
}

.spin {
  animation: spin 1s linear infinite;
  color: var(--accent);
}

@keyframes spin {
  to { transform: rotate(360deg); }
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
