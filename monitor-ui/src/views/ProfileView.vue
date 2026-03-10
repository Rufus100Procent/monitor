<script setup lang="ts">
import { computed } from 'vue'
import { useAuth } from '../composables/useAuth'
import { useDisplayTimezone } from '../composables/useDisplayTimezone'
import { useRouter } from 'vue-router'

const { tokenPayload, clearToken } = useAuth()
const { selectedIana } = useDisplayTimezone()
const router = useRouter()

function formatDate(date: string | number | undefined): string {
  if (!date) return '—'
  const d = typeof date === 'number' ? new Date(date * 1000) : new Date(date)
  return d.toLocaleString('en-GB', {
    timeZone: selectedIana.value,
    year: 'numeric', month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
  })
}

const createdAt = computed(() => formatDate(tokenPayload.value?.createdAt))
const expiresAt = computed(() => formatDate(tokenPayload.value?.exp))

function logout() {
  clearToken()
  router.push('/')
}
</script>

<template>
  <div class="profile">
    <div class="section">
      <div class="section-title">Account</div>
      <div class="card">
        <div class="row">
          <span class="label">User ID</span>
          <span class="value mono">{{ tokenPayload?.userId ?? '—' }}</span>
        </div>
        <div class="row">
          <span class="label">Username</span>
          <span class="value">{{ tokenPayload?.sub ?? '—' }}</span>
        </div>
        <div class="row">
          <span class="label">Role</span>
          <span class="value">{{ tokenPayload?.role ?? '—' }}</span>
        </div>
        <div class="row">
          <span class="label">Created at</span>
          <span class="value">{{ createdAt }}</span>
        </div>
        <div class="row">
          <span class="label">Session expires</span>
          <span class="value">{{ expiresAt }}</span>
        </div>
      </div>
    </div>

    <div class="section danger-section">
      <div class="section-title">Session</div>
      <div class="card">
        <div class="danger-row">
          <div>
            <div class="danger-label">Sign out</div>
            <div class="danger-hint">Clears your session token from this device.</div>
          </div>
          <button class="logout-btn" @click="logout">Sign out</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile {
  display: flex;
  flex-direction: column;
  gap: 28px;
  max-width: 600px;
}

.section-title {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--text-muted);
  margin-bottom: 10px;
}

.card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 13px 18px;
  border-bottom: 1px solid var(--border);
  gap: 16px;
}

.row:last-child {
  border-bottom: none;
}

.label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.value {
  font-size: 13px;
  color: var(--text-primary);
  text-align: right;
  word-break: break-all;
}

.value.mono {
  font-family: monospace;
  font-size: 12px;
  color: var(--text-muted);
}

.danger-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  gap: 16px;
}

.danger-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.danger-hint {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

.logout-btn {
  padding: 7px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  color: hsl(0, 65%, 52%);
  border: 1px solid hsl(0, 65%, 80%);
  background: hsl(0, 80%, 98%);
  flex-shrink: 0;
  transition: background 0.12s;
}

.logout-btn:hover {
  background: hsl(0, 70%, 94%);
}

:root.dark .logout-btn {
  background: hsl(0, 40%, 16%);
  border-color: hsl(0, 45%, 28%);
  color: hsl(0, 60%, 65%);
}

:root.dark .logout-btn:hover {
  background: hsl(0, 40%, 20%);
}
</style>
