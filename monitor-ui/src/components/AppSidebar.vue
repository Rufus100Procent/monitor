<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { LayoutDashboard, HelpCircle, UserRound, Info, SatelliteDish, Settings } from 'lucide-vue-next'
import { useSidebar } from '../composables/useSidebar'
import { useServers } from '../composables/useServers'

const { open: sidebarOpen, close } = useSidebar()
const { servers, loading, ensureLoaded } = useServers()
const route = useRoute()

ensureLoaded()

// Close sidebar on navigation (mobile)
watch(() => route.path, close)

// Accordion open state per server id
const openServers = ref<Set<string>>(new Set())

function toggleServer(id: string) {
  const next = new Set(openServers.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  openServers.value = next
}

// Auto-expand the server whose sub-page is active
const activeServerId = computed(() => route.params.id as string | undefined)
watch(activeServerId, (id) => {
  if (id) {
    const next = new Set(openServers.value)
    next.add(id)
    openServers.value = next
  }
}, { immediate: true })
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

        <!-- Loading -->
        <div v-if="loading" class="srv-loading">Loading…</div>

        <!-- Server accordions -->
        <div v-for="server in servers" :key="server.id" class="srv-group">
          <button class="srv-header" @click="toggleServer(server.id)">
            <span class="srv-dot" :class="server.status === 'UP' ? 'up' : 'down'" />
            <span class="srv-name">{{ server.appName }}</span>
            <span class="i-arrow" :class="{ open: openServers.has(server.id) }">▼</span>
          </button>

          <div class="gr-wrap" :class="{ open: openServers.has(server.id) }">
            <div class="gr-inner">
              <div class="srv-subnav">
                <RouterLink :to="`/server/${server.id}/basics`" class="nav-item sub" activeClass="active">
                  <Info :size="15" />
                  <span>Basics</span>
                </RouterLink>
                <RouterLink :to="`/server/${server.id}/track-me`" class="nav-item sub" activeClass="active">
                  <SatelliteDish :size="15" />
                  <span>Track Me</span>
                </RouterLink>
                <RouterLink :to="`/server/${server.id}/settings`" class="nav-item sub" activeClass="active">
                  <Settings :size="15" />
                  <span>Settings</span>
                </RouterLink>
              </div>
            </div>
          </div>
        </div>
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
    hsl(270, 70%, 55%) 0%,
    hsl(300, 65%, 58%) 100%
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

/* Sub nav items indented */
.nav-item.sub {
  padding-left: 22px;
  font-size: 12px;
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

.srv-loading {
  padding: 4px 10px;
  font-size: 12px;
  color: var(--text-muted);
}

/* Server accordion header */
.srv-group {
  display: flex;
  flex-direction: column;
}

.srv-header {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 7px 10px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  transition: background 0.12s, color 0.12s;
  width: 100%;
  text-align: left;
}

.srv-header:hover {
  background: var(--nav-hover);
  color: var(--text-primary);
}

.srv-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.srv-dot.up   { background: hsl(142, 65%, 42%); }
.srv-dot.down { background: hsl(0, 65%, 52%); }

.srv-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.i-arrow {
  font-size: 0.52rem;
  color: var(--text-muted);
  transition: transform 0.25s ease, color 0.15s;
  flex-shrink: 0;
}

.i-arrow.open {
  transform: rotate(180deg);
  color: var(--text-secondary);
}

/* Accordion expand */
.gr-wrap {
  display: grid;
  grid-template-rows: 0fr;
  transition: grid-template-rows 0.28s ease;
}

.gr-wrap.open {
  grid-template-rows: 1fr;
}

.gr-inner {
  overflow: hidden;
}

.srv-subnav {
  display: flex;
  flex-direction: column;
  gap: 1px;
  padding: 2px 0 4px;
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
