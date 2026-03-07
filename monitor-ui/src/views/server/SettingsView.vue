<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { LoaderCircle } from 'lucide-vue-next'
import { updateServer, deleteServer } from '../../api/servers'
import { useServers } from '../../composables/useServers'

const route = useRoute()
const router = useRouter()
const { servers, ensureLoaded, updateServerInList, removeServer } = useServers()

const serverId = computed(() => route.params.id as string)
const server = computed(() => servers.value.find(s => s.id === serverId.value))

onMounted(ensureLoaded)

// Form state
const form = ref({
  appName: '',
  appVersion: '',
  pollIntervalSeconds: 10,
  pause: false,
})

// Populate form when server is found
watch(server, (s) => {
  if (s) {
    form.value = {
      appName: s.appName,
      appVersion: s.appVersion ?? '',
      pollIntervalSeconds: s.pollIntervalSeconds,
      pause: s.pause,
    }
  }
}, { immediate: true })

const isDirty = computed(() => {
  const s = server.value
  if (!s) return false
  return (
    form.value.appName !== s.appName ||
    form.value.appVersion !== (s.appVersion ?? '') ||
    form.value.pollIntervalSeconds !== s.pollIntervalSeconds ||
    form.value.pause !== s.pause
  )
})

// ── Update
const updating = ref(false)
const updateError = ref('')

async function confirmUpdate() {
  const s = server.value
  if (!s || !isDirty.value) return
  updating.value = true
  updateError.value = ''
  try {
    const updated = await updateServer({ id: s.id, ...form.value })
    updateServerInList(updated)
  } catch (e) {
    updateError.value = e instanceof Error ? e.message : 'Update failed.'
  } finally {
    updating.value = false
  }
}

// Delete
const deleting = ref(false)
const deleteError = ref('')

async function confirmDelete() {
  const s = server.value
  if (!s) return
  deleting.value = true
  deleteError.value = ''
  try {
    await deleteServer(s.id)
    removeServer(s.id)
    router.push('/dashboard')
  } catch (e) {
    deleteError.value = e instanceof Error ? e.message : 'Delete failed.'
    deleting.value = false
  }
}
</script>

<template>
  <div class="settings">

    <!-- Not found / loading -->
    <div v-if="!server" class="state-box">
      <LoaderCircle :size="24" class="spin" />
    </div>

    <template v-else>

      <!-- Div 1: Server ID -->
      <div class="settings-section">
        <h2 class="section-title">Server Identity</h2>
        <div class="id-row">
          <span class="id-label">Server ID</span>
          <span class="id-value">{{ server.id }}</span>
        </div>
      </div>

      <!-- Div 2: Editable fields -->
      <div class="settings-section">
        <h2 class="section-title">Configuration</h2>

        <div class="fields">
          <div class="field">
            <label>App Name</label>
            <input v-model="form.appName" type="text" />
          </div>

          <div class="field">
            <label>App Version</label>
            <input v-model="form.appVersion" type="text" placeholder="e.g. 1.0.0" />
          </div>

          <div class="field">
            <label>Poll Interval (seconds)</label>
            <input v-model.number="form.pollIntervalSeconds" type="number" min="1" />
          </div>

          <div class="field">
            <label>Polling</label>
            <label class="toggle">
              <input type="checkbox" v-model="form.pause" class="toggle-input" />
              <span class="toggle-track">
                <span class="toggle-thumb" />
              </span>
              <span class="toggle-label-text">{{ form.pause ? 'Paused' : 'Active' }}</span>
            </label>
          </div>
        </div>

        <div v-if="updateError" class="inline-error">{{ updateError }}</div>

        <button
          class="btn-confirm"
          :disabled="!isDirty || updating"
          @click="confirmUpdate"
        >
          <LoaderCircle v-if="updating" :size="14" class="spin" />
          {{ updating ? 'Saving…' : 'Confirm' }}
        </button>
      </div>

      <!-- Div 3: Delete -->
      <div class="settings-section danger-section">
        <h2 class="section-title danger-title">Danger Zone</h2>
        <p class="danger-desc">
          Permanently removes this server and all its Snapshots from Monitor UI.
          This action cannot be undone.
        </p>

        <div v-if="deleteError" class="inline-error">{{ deleteError }}</div>

        <button class="btn-delete" :disabled="deleting" @click="confirmDelete">
          <LoaderCircle v-if="deleting" :size="14" class="spin" />
          {{ deleting ? 'Deleting…' : 'Delete Server' }}
        </button>
      </div>

    </template>
  </div>
</template>

<style scoped>
.settings {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 560px;
}

.state-box {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: var(--text-muted);
}

/* Section cards */
.settings-section {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

/* ID row */
.id-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.id-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
}

.id-value {
  font-size: 13px;
  color: var(--text-primary);
  font-family: monospace;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 7px;
  padding: 8px 12px;
  word-break: break-all;
}

/* Fields */
.fields {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.field label:first-child {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}

.field input[type='text'],
.field input[type='number'] {
  padding: 8px 11px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg);
  color: var(--text-primary);
  font-size: 13px;
  font-family: inherit;
  outline: none;
  transition: border-color 0.15s;
}

.field input[type='text']:focus,
.field input[type='number']:focus {
  border-color: var(--accent);
}

/* Toggle switch */
.toggle {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  width: fit-content;
}

.toggle-input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.toggle-track {
  position: relative;
  width: 36px;
  height: 20px;
  border-radius: 20px;
  background: hsl(220, 15%, 78%);
  transition: background 0.2s;
  flex-shrink: 0;
}

.toggle-input:checked + .toggle-track {
  background: var(--accent);
}

.toggle-thumb {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: hsl(0, 0%, 100%);
  transition: transform 0.2s;
  box-shadow: 0 1px 3px hsl(220, 20%, 10%, 0.2);
}

.toggle-input:checked + .toggle-track .toggle-thumb {
  transform: translateX(16px);
}

.toggle-label-text {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

/* Confirm button */
.btn-confirm {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px 20px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  color: hsl(0, 0%, 100%);
  background: var(--accent);
  transition: opacity 0.15s;
  align-self: flex-start;
}

.btn-confirm:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.btn-confirm:not(:disabled):hover {
  opacity: 0.88;
}

/* Error */
.inline-error {
  font-size: 13px;
  color: hsl(0, 65%, 50%);
  background: hsl(0, 65%, 97%);
  border: 1px solid hsl(0, 65%, 88%);
  border-radius: 8px;
  padding: 9px 12px;
}

:root.dark .inline-error {
  background: hsl(0, 40%, 14%);
  border-color: hsl(0, 40%, 24%);
  color: hsl(0, 65%, 65%);
}

/* Danger section */
.danger-section {
  border-color: hsl(0, 65%, 88%);
}

:root.dark .danger-section {
  border-color: hsl(0, 40%, 28%);
}

.danger-title {
  color: hsl(0, 65%, 50%);
}

:root.dark .danger-title {
  color: hsl(0, 65%, 60%);
}

.danger-desc {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.btn-delete {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px 20px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  color: hsl(0, 0%, 100%);
  background: hsl(0, 65%, 52%);
  transition: opacity 0.15s;
  align-self: flex-start;
}

.btn-delete:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-delete:not(:disabled):hover {
  opacity: 0.88;
}

/* Spinner */
.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
