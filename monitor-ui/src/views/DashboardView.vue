<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, LoaderCircle } from 'lucide-vue-next'
import { useServers } from '../composables/useServers'
import { useDisplayTimezone } from '../composables/useDisplayTimezone'
import RegisterServerModal from '../components/RegisterServerModal.vue'

const router = useRouter()

const { servers, loading, error: fetchError, ensureLoaded } = useServers()
const { selectedIana } = useDisplayTimezone()

onMounted(ensureLoaded)

function formatBytes(bytes: number): string {
  return (bytes / 1024 / 1024 / 1024).toFixed(1) + ' GB'
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-GB', {
    timeZone: selectedIana.value, day: '2-digit', month: '2-digit', year: '2-digit',
  })
}

function formatTime(iso: string): string {
  return new Date(iso).toLocaleTimeString('en-GB', {
    timeZone: selectedIana.value, hour: '2-digit', minute: '2-digit', second: '2-digit',
  })
}

const showModal = ref(false)
</script>

<template>
  <div class="dashboard">

    <!-- Header -->
    <div class="dash-header">
      <h1 class="dash-title">Servers</h1>
      <button class="register-btn" @click="showModal = true">
        <Plus :size="15" />
        Register Server
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="state-box">
      <LoaderCircle :size="26" class="spin" />
      <span>Loading servers…</span>
    </div>

    <!-- Fetch error -->
    <div v-else-if="fetchError" class="state-box error-text">{{ fetchError }}</div>

    <!-- Empty -->
    <div v-else-if="servers.length === 0" class="state-box muted-text">
      No servers registered yet.
    </div>

    <!-- Server grid -->
    <div v-else class="server-grid">
      <div v-for="server in servers" :key="server.id" class="server-card">

        <div class="card-head">
          <button class="app-name" @click="router.push(`/server/${server.id}/settings`)">{{ server.appName }}</button>
          <span class="status-badge" :class="server.status === 'UP' ? 'up' : 'down'">{{ server.status }}</span>
        </div>

        <div v-if="server.appVersion" class="app-version">v{{ server.appVersion }}</div>

        <div class="stats">
          <div class="stat-row">
            <span class="stat-label">Total CPU</span>
            <span class="stat-value">{{ server.cpuCoreCount }} cores</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">Max Heap</span>
            <span class="stat-value">{{ formatBytes(server.memoryMaxBytes) }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">Polling</span>
            <span class="stat-value" :class="server.pause ? 'paused' : 'active'">
              {{ server.pause ? 'Paused' : 'Active' }}
            </span>
          </div>
          <div class="stat-row">
            <span class="stat-label">Interval</span>
            <span class="stat-value">{{ server.pollIntervalSeconds }}s</span>
          </div>
        </div>

        <hr class="card-divider" />

        <div class="dates">
          <div class="date-row">
            <span class="date-label">Registered</span>
            <span class="date-value">{{ formatDate(server.registeredAt) }}</span>
          </div>
          <div class="date-row">
            <span class="date-label">Last seen up</span>
            <span class="date-value">{{ formatTime(server.lastSeenUp) }} · {{ formatDate(server.lastSeenUp) }}</span>
          </div>
          <div class="date-row">
            <span class="date-label">Last polled</span>
            <span class="date-value">{{ formatTime(server.lastPolledAt) }} · {{ formatDate(server.lastPolledAt) }}</span>
          </div>
        </div>

      </div>
    </div>

  </div>

  <RegisterServerModal v-model="showModal" />
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.dash-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.dash-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: -0.01em;
}

.register-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  color: hsl(0, 0%, 100%);
  background: var(--accent);
  transition: opacity 0.15s;
}
.register-btn:hover { opacity: 0.88; }

.state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 60px 0;
  font-size: 14px;
  color: var(--text-muted);
}

.error-text  { color: hsl(0, 65%, 50%); }
.muted-text  { color: var(--text-muted); }

.spin { animation: spin 1s linear infinite; color: var(--accent); }
@keyframes spin { to { transform: rotate(360deg); } }

.server-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.server-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.app-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  text-align: left;
  line-height: 1.3;
  transition: transform 0.15s ease, color 0.15s;
  cursor: pointer;
}
.app-name:hover { transform: scale(1.04); color: var(--accent); }

.status-badge {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  padding: 3px 8px;
  border-radius: 20px;
  flex-shrink: 0;
}
.status-badge.up   { background: hsl(142,60%,92%); color: hsl(142,65%,30%); }
.status-badge.down { background: hsl(0,65%,93%);   color: hsl(0,65%,40%); }
:root.dark .status-badge.up   { background: hsl(142,40%,16%); color: hsl(142,60%,60%); }
:root.dark .status-badge.down { background: hsl(0,40%,18%);   color: hsl(0,60%,62%); }

.app-version { font-size: 12px; color: var(--text-muted); margin-top: -6px; }

.stats { display: flex; flex-direction: column; gap: 6px; }
.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}
.stat-label  { color: var(--text-secondary); }
.stat-value  { font-weight: 600; color: var(--text-primary); }
.stat-value.active { color: hsl(142,65%,35%); }
.stat-value.paused { color: hsl(38,80%,45%); }
:root.dark .stat-value.active { color: hsl(142,60%,58%); }
:root.dark .stat-value.paused { color: hsl(38,80%,60%); }

.card-divider { margin: 0; }

.dates { display: flex; flex-direction: column; gap: 5px; }
.date-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}
.date-label  { color: var(--text-muted); }
.date-value  { color: var(--text-secondary); font-variant-numeric: tabular-nums; }
</style>
