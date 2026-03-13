<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { LoaderCircle, Copy, Check } from 'lucide-vue-next'
import { useAuth } from '../../composables/useAuth'
import { useDisplayTimezone } from '../../composables/useDisplayTimezone'
import { generateUserSecret, deleteUser, getUserSnapshotSize } from '../../api/api'

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

// Snapshot size
const snapshotSize   = ref<string | null>(null)
const loadingSize    = ref(false)

onMounted(async () => {
  loadingSize.value = true
  try {
    snapshotSize.value = await getUserSnapshotSize()
  } catch {
    snapshotSize.value = null
  } finally {
    loadingSize.value = false
  }
})

// Generate secret
const generatedSecret  = ref('')
const generatingSecret = ref(false)
const generateError    = ref('')
const secretCopied     = ref(false)

async function doGenerateSecret() {
  generatingSecret.value = true
  generateError.value    = ''
  generatedSecret.value  = ''
  try {
    generatedSecret.value = await generateUserSecret()
  } catch (e) {
    generateError.value = e instanceof Error ? e.message : 'Failed to generate secret.'
  } finally {
    generatingSecret.value = false
  }
}

async function copySecret() {
  await navigator.clipboard.writeText(generatedSecret.value)
  secretCopied.value = true
  setTimeout(() => { secretCopied.value = false }, 2000)
}

// Sign out
function logout() {
  clearToken()
  router.push('/')
}

// Delete user
const confirmingDelete = ref(false)
const deleting         = ref(false)
const deleteError      = ref('')

async function confirmDelete() {
  deleting.value     = true
  deleteError.value  = ''
  try {
    await deleteUser()
    clearToken()
    router.push('/')
  } catch (e) {
    deleteError.value = e instanceof Error ? e.message : 'Delete failed.'
    deleting.value    = false
    confirmingDelete.value = false
  }
}
</script>

<template>
  <div class="profile">

    <!-- Account -->
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

    <!-- Snapshot Data -->
    <div class="section">
      <div class="section-title">Snapshot Data</div>
      <div class="card">
        <div class="row">
          <span class="label">Total size across all servers</span>
          <span class="value">
            <LoaderCircle v-if="loadingSize" :size="13" class="spin" />
            <span v-else>{{ snapshotSize ?? '—' }}</span>
          </span>
        </div>
      </div>
    </div>

    <!-- Secret -->
    <div class="section">
      <div class="section-title">Secret</div>
      <div class="card pad">
        <p class="hint-text">Generate a personal secret key. The previous secret will be invalidated immediately.</p>

        <div v-if="generatedSecret" class="secret-reveal">
          <p class="secret-notice">Make sure to copy your token now as you will not be able to see it again.</p>
          <div class="secret-row">
            <code class="secret-value">{{ generatedSecret }}</code>
            <button class="btn-copy" :class="{ copied: secretCopied }" @click="copySecret" :title="secretCopied ? 'Copied!' : 'Copy secret'">
              <Check v-if="secretCopied" :size="15" />
              <Copy v-else :size="15" />
            </button>
          </div>
        </div>

        <div v-if="generateError" class="inline-error">{{ generateError }}</div>

        <button class="btn-action" :disabled="generatingSecret" @click="doGenerateSecret">
          <LoaderCircle v-if="generatingSecret" :size="14" class="spin" />
          {{ generatingSecret ? 'Generating…' : 'Generate New Secret' }}
        </button>
      </div>
    </div>

    <!-- Session -->
    <div class="section">
      <div class="section-title">Session</div>
      <div class="card">
        <div class="action-row">
          <div>
            <div class="action-label">Sign out</div>
            <div class="action-hint">Clears your session token from this device.</div>
          </div>
          <button class="btn-signout" @click="logout">Sign out</button>
        </div>
      </div>
    </div>

    <!-- Danger Zone -->
    <div class="section">
      <div class="section-title danger-title">Danger Zone</div>
      <div class="card danger-card pad">
        <p class="hint-text danger-hint-text">
          Permanently deletes your account and all registered servers including their snapshots.
          This action cannot be undone.
        </p>

        <div v-if="deleteError" class="inline-error">{{ deleteError }}</div>

        <!-- Inline confirm -->
        <div v-if="confirmingDelete" class="confirm-row">
          <span class="confirm-text">Are you sure? This is permanent.</span>
          <button class="btn-cancel-action" @click="confirmingDelete = false">Cancel</button>
          <button class="btn-delete" :disabled="deleting" @click="confirmDelete">
            <LoaderCircle v-if="deleting" :size="14" class="spin" />
            {{ deleting ? 'Deleting…' : 'Yes, delete' }}
          </button>
        </div>
        <button v-else class="btn-delete" @click="confirmingDelete = true">Delete Account</button>
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

.danger-title { color: hsl(0, 65%, 52%); }
:root.dark .danger-title { color: hsl(0, 65%, 62%); }

.card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
}

.card.pad { padding: 18px; display: flex; flex-direction: column; gap: 14px; }

.danger-card { border-color: hsl(0, 65%, 88%); }
:root.dark .danger-card { border-color: hsl(0, 40%, 28%); }

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 13px 18px;
  border-bottom: 1px solid var(--border);
  gap: 16px;
}
.row:last-child { border-bottom: none; }

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
  display: flex;
  align-items: center;
  gap: 6px;
}

.value.mono {
  font-family: monospace;
  font-size: 12px;
  color: var(--text-muted);
}

.hint-text {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
}

.danger-hint-text { color: var(--text-secondary); }

/* Secret */
.secret-reveal {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.secret-notice {
  font-size: 13px;
  color: var(--text-secondary);
  background: hsl(38, 90%, 96%);
  border: 1px solid hsl(38, 80%, 82%);
  border-radius: 8px;
  padding: 10px 13px;
  line-height: 1.5;
}
:root.dark .secret-notice {
  background: hsl(38, 40%, 14%);
  border-color: hsl(38, 40%, 26%);
  color: hsl(38, 70%, 75%);
}

.secret-row {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 10px 12px;
}

.secret-value {
  flex: 1;
  font-family: monospace;
  font-size: 12px;
  color: var(--text-primary);
  word-break: break-all;
}

.btn-copy {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 7px;
  color: var(--text-muted);
  flex-shrink: 0;
  transition: background 0.15s, color 0.15s;
}
.btn-copy:hover { background: var(--nav-hover); color: var(--text-primary); }
.btn-copy.copied { color: hsl(142, 60%, 40%); }

/* Buttons */
.btn-action {
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
.btn-action:disabled             { opacity: 0.4; cursor: not-allowed; }
.btn-action:not(:disabled):hover { opacity: 0.88; }

/* Action row (sign out) */
.action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  gap: 16px;
}

.action-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.action-hint {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

.btn-signout {
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
.btn-signout:hover { background: hsl(0, 70%, 94%); }
:root.dark .btn-signout {
  background: hsl(0, 40%, 16%);
  border-color: hsl(0, 45%, 28%);
  color: hsl(0, 60%, 65%);
}
:root.dark .btn-signout:hover { background: hsl(0, 40%, 20%); }

/* Inline confirm */
.confirm-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.confirm-text {
  font-size: 13px;
  color: var(--text-secondary);
  flex: 1;
}

.btn-cancel-action {
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  transition: background 0.15s;
}
.btn-cancel-action:hover { background: var(--nav-hover); }

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
.btn-delete:disabled             { opacity: 0.5; cursor: not-allowed; }
.btn-delete:not(:disabled):hover { opacity: 0.88; }

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

.spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
