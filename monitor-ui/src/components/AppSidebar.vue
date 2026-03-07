<script setup lang="ts">
import { watch } from 'vue'
import { useRoute } from 'vue-router'
import { LayoutDashboard, HelpCircle, UserRound } from 'lucide-vue-next'
import { useSidebar } from '../composables/useSidebar'

const { open: sidebarOpen, close } = useSidebar()
const route = useRoute()

// Close sidebar on navigation (mobile)
watch(() => route.path, close)
</script>

<template>
  <aside class="sidebar" :class="{ 'mobile-open': sidebarOpen }">
    <div class="sidebar-top">
      <div class="brand">Monitor UI</div>
      <hr />
      <nav class="nav">
        <RouterLink to="/dashboard" class="nav-item" activeClass="active">
          <LayoutDashboard :size="17" />
          <span>Dashboard</span>
        </RouterLink>

        <hr class="nav-sep" />

        <div class="nav-label">Servers</div>
      </nav>
    </div>

    <div class="sidebar-bottom">
      <RouterLink to="/faq" class="nav-item" activeClass="active">
        <HelpCircle :size="17" />
        <span>FAQ</span>
      </RouterLink>
      <hr />
      <RouterLink to="/profile" class="nav-item" activeClass="active">
        <UserRound :size="17" />
        <span>Profile</span>
      </RouterLink>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 220px;
  height: 100%;
  background: var(--sidebar-bg);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-top {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 16px 12px 12px;
  overflow-y: auto;
}

.sidebar-bottom {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 12px;
}

.sidebar-bottom hr {
  margin: 6px 0;
}

.brand {
  font-size: 15px;
  font-weight: 700;
  padding: 2px 8px 10px;
  letter-spacing: -0.01em;
  background: linear-gradient(
    135deg,
    hsl(172, 62%, 24%) 0%,
    hsl(300, 8%, 72%) 100%
  );
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

hr {
  margin: 6px 0;
}

.nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 8px 10px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  transition: background 0.12s, color 0.12s;
}

.nav-item:hover {
  background: var(--nav-hover);
  color: var(--text-primary);
}

.nav-item.active {
  background: linear-gradient(
    to right,
    hsl(270, 80%, 95%) 0%,
    hsl(270, 60%, 97%) 60%,
    transparent 100%
  );
  color: var(--nav-active-text);
  font-weight: 600;
}

:root.dark .nav-item.active {
  background: linear-gradient(
    to right,
    hsl(270, 50%, 20%) 0%,
    hsl(270, 40%, 16%) 60%,
    transparent 100%
  );
}

.nav-sep {
  margin: 8px 0;
}

.nav-label {
  padding: 4px 10px 2px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--text-muted);
}

@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    top: 0;
    left: 0;
    height: 100%;
    z-index: 200;
    transform: translateX(-100%);
    transition: transform 0.25s ease;
    box-shadow: 4px 0 24px hsl(220, 20%, 10%, 0.15);
  }

  .sidebar.mobile-open {
    transform: translateX(0);
  }
}
</style>
