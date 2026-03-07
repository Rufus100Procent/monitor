<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, LoaderCircle, X } from 'lucide-vue-next'
import { registerServer } from '../api/servers'
import type { RegisterServerBody } from '../api/servers'
import { useServers } from '../composables/useServers'
import { useDisplayTimezone } from '../composables/useDisplayTimezone'

const router = useRouter()

// Shared server state
const { servers, loading, error: fetchError, fetch: fetchServers, ensureLoaded } = useServers()
const { selectedIana } = useDisplayTimezone()

onMounted(ensureLoaded)

// Formatting helpers
function formatBytes(bytes: number): string {
  return (bytes / 1024 / 1024 / 1024).toFixed(1) + ' GB'
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-GB', {
    timeZone: selectedIana.value,
    day: '2-digit',
    month: '2-digit',
    year: '2-digit',
  })
}

function formatTime(iso: string): string {
  return new Date(iso).toLocaleTimeString('en-GB', {
    timeZone: selectedIana.value,
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

// ── Register modal
const showModal = ref(false)
const submitting = ref(false)
const submitError = ref('')

const defaultForm = (): RegisterServerBody => ({
  actuatorPath: '/actuator',
  baseUrl: '',
  appName: '',
  appVersion: '',
  pollIntervalSeconds: 10,
})

const form = ref<RegisterServerBody>(defaultForm())

function openModal() {
  form.value = defaultForm()
  submitError.value = ''
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

async function submitRegister() {
  submitting.value = true
  submitError.value = ''
  try {
    await registerServer(form.value)
    closeModal()
    await fetchServers()
  } catch (e: unknown) {
    submitError.value = e instanceof Error ? e.message : 'Registration failed.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="dashboard">

    <!-- Header -->
    <div class="dash-header">
      <h1 class="dash-title">Servers</h1>
      <button class="register-btn" @click="openModal">
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
    <div v-else-if="fetchError" class="state-box error-text">
      {{ fetchError }}
    </div>

    <!-- Empty -->
    <div v-else-if="servers.length === 0" class="state-box muted-text">
      No servers registered yet.
    </div>

    <!-- Server grid -->
    <div v-else class="server-grid">
      <div v-for="server in servers" :key="server.id" class="server-card">

        <!-- Card head: name + status -->
        <div class="card-head">
          <button class="app-name" @click="router.push(`/server/${server.id}/settings`)">{{ server.appName }}</button>
          <span class="status-badge" :class="server.status === 'UP' ? 'up' : 'down'">
            {{ server.status }}
          </span>
        </div>

        <!-- Version -->
        <div v-if="server.appVersion" class="app-version">v{{ server.appVersion }}</div>

        <!-- Stats -->
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

        <!-- Dates -->
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

  <!-- Register modal -->
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="showModal" class="modal-backdrop" @click.self="closeModal">
        <div class="modal" role="dialog" aria-modal="true">

          <div class="modal-head">
            <span class="modal-title">Register Server</span>
            <button class="modal-close" @click="closeModal">
              <X :size="18" />
            </button>
          </div>

          <form class="modal-form" @submit.prevent="submitRegister">

            <div class="field">
              <label>App Name</label>
              <input v-model="form.appName" type="text" placeholder="Music Service" required />
            </div>

            <div class="field">
              <label>App Version</label>
              <input v-model="form.appVersion" type="text" placeholder="1.0.0" required />
            </div>

            <div class="field">
              <label>Base URL</label>
              <input v-model="form.baseUrl" type="text" placeholder="http://localhost:8082" required />
            </div>

            <div class="field">
              <label>Actuator Path</label>
              <input v-model="form.actuatorPath" type="text" placeholder="/actuator" required />
            </div>

            <div class="field">
              <label>Poll Interval (seconds)</label>
              <input v-model.number="form.pollIntervalSeconds" type="number" min="1" required />
            </div>

            <div v-if="submitError" class="submit-error">{{ submitError }}</div>

            <div class="modal-actions">
              <button type="button" class="btn-cancel" @click="closeModal">Cancel</button>
              <button type="submit" class="btn-submit" :disabled="submitting">
                <LoaderCircle v-if="submitting" :size="14" class="spin" />
                {{ submitting ? 'Registering…' : 'Register' }}
              </button>
            </div>

          </form>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* Header */
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

.register-btn:hover {
  opacity: 0.88;
}

/* States */
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

.error-text { color: hsl(0, 65%, 50%); }

.spin {
  animation: spin 1s linear infinite;
  color: var(--accent);
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Grid */
.server-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

/* Card */
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
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
  text-align: left;
  line-height: 1.3;
  transition: transform 0.15s ease, color 0.15s;
  cursor: pointer;
}

.app-name:hover {
  transform: scale(1.04);
  color: var(--accent);
}

.status-badge {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  padding: 3px 8px;
  border-radius: 20px;
  flex-shrink: 0;
}

.status-badge.up {
  background: hsl(142, 60%, 92%);
  color: hsl(142, 65%, 30%);
}

.status-badge.down {
  background: hsl(0, 65%, 93%);
  color: hsl(0, 65%, 40%);
}

:root.dark .status-badge.up {
  background: hsl(142, 40%, 16%);
  color: hsl(142, 60%, 60%);
}

:root.dark .status-badge.down {
  background: hsl(0, 40%, 18%);
  color: hsl(0, 60%, 62%);
}

.app-version {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: -6px;
}

/* Stats */
.stats {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}

.stat-label {
  color: var(--text-secondary);
}

.stat-value {
  font-weight: 600;
  color: var(--text-primary);
}

.stat-value.active { color: hsl(142, 65%, 35%); }
.stat-value.paused { color: hsl(38, 80%, 45%); }

:root.dark .stat-value.active { color: hsl(142, 60%, 58%); }
:root.dark .stat-value.paused { color: hsl(38, 80%, 60%); }

.card-divider {
  margin: 0;
}

/* Dates */
.dates {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.date-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}

.date-label {
  color: var(--text-muted);
}

.date-value {
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
}

/* Modal */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: hsl(220, 20%, 5%, 0.5);
  z-index: 300;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.modal {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 14px;
  width: 100%;
  max-width: 420px;
  box-shadow: 0 24px 48px hsl(220, 20%, 5%, 0.2);
}

.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px 16px;
  border-bottom: 1px solid var(--border);
}

.modal-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.modal-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 7px;
  color: var(--text-muted);
  transition: background 0.15s, color 0.15s;
}

.modal-close:hover {
  background: var(--nav-hover);
  color: var(--text-primary);
}

.modal-form {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.field label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}

.field input {
  padding: 8px 11px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg);
  color: var(--text-primary);
  font-size: 13px;
  font-family: inherit;
  transition: border-color 0.15s;
  outline: none;
}

.field input:focus {
  border-color: var(--accent);
}

.field input::placeholder {
  color: var(--text-muted);
}

.submit-error {
  font-size: 13px;
  color: hsl(0, 65%, 50%);
  background: hsl(0, 65%, 97%);
  border: 1px solid hsl(0, 65%, 88%);
  border-radius: 8px;
  padding: 9px 12px;
}

:root.dark .submit-error {
  background: hsl(0, 40%, 14%);
  border-color: hsl(0, 40%, 24%);
  color: hsl(0, 65%, 65%);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 4px;
}

.btn-cancel {
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  transition: background 0.15s;
}

.btn-cancel:hover {
  background: var(--nav-hover);
}

.btn-submit {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  color: hsl(0, 0%, 100%);
  background: var(--accent);
  transition: opacity 0.15s;
}

.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-submit:not(:disabled):hover {
  opacity: 0.88;
}

/* Modal transition */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s;
}
.modal-enter-active .modal,
.modal-leave-active .modal {
  transition: transform 0.2s ease, opacity 0.2s;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
.modal-enter-from .modal,
.modal-leave-to .modal {
  transform: scale(0.96) translateY(8px);
  opacity: 0;
}
</style>
