<script setup lang="ts">
import { ref, watch } from 'vue'
import { LoaderCircle, X } from 'lucide-vue-next'
import { registerServer } from '../api/servers'
import type { RegisterServerBody } from '../api/servers'
import { useServers } from '../composables/useServers'

const props = defineProps<{
  modelValue: boolean
  prefillUrl?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  registered: []
}>()

const { fetch: fetchServers } = useServers()

const submitting  = ref(false)
const submitError = ref('')

const defaultForm = (): RegisterServerBody => ({
  actuatorPath: '/actuator',
  baseUrl: '',
  appName: '',
  appVersion: '',
  pollIntervalSeconds: 10,
})

const form = ref<RegisterServerBody>(defaultForm())

function parseActuatorUrl(raw: string): { baseUrl: string; actuatorPath: string } {
  try {
    const u = new URL(raw.trim())
    return { baseUrl: u.origin, actuatorPath: u.pathname || '/actuator' }
  } catch {
    return { baseUrl: raw.trim(), actuatorPath: '/actuator' }
  }
}

watch(() => props.modelValue, (open) => {
  if (open) {
    form.value = defaultForm()
    submitError.value = ''
    if (props.prefillUrl) {
      const { baseUrl, actuatorPath } = parseActuatorUrl(props.prefillUrl)
      form.value.baseUrl     = baseUrl
      form.value.actuatorPath = actuatorPath
    }
  }
})

function close() {
  emit('update:modelValue', false)
}

async function submit() {
  submitting.value  = true
  submitError.value = ''
  try {
    await registerServer(form.value)
    await fetchServers()
    close()
    emit('registered')
  } catch (e: unknown) {
    submitError.value = e instanceof Error ? e.message : 'Registration failed.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="modelValue" class="modal-backdrop" @click.self="close">
        <div class="modal" role="dialog" aria-modal="true">

          <div class="modal-head">
            <span class="modal-title">Register Server</span>
            <button class="modal-close" @click="close">
              <X :size="18" />
            </button>
          </div>

          <form class="modal-form" @submit.prevent="submit">

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
              <button type="button" class="btn-cancel" @click="close">Cancel</button>
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
.field input:focus     { border-color: var(--accent); }
.field input::placeholder { color: var(--text-muted); }

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
.btn-cancel:hover { background: var(--nav-hover); }

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
.btn-submit:disabled           { opacity: 0.6; cursor: not-allowed; }
.btn-submit:not(:disabled):hover { opacity: 0.88; }

.spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.modal-enter-active,
.modal-leave-active { transition: opacity 0.2s; }
.modal-enter-active .modal,
.modal-leave-active .modal { transition: transform 0.2s ease, opacity 0.2s; }
.modal-enter-from,
.modal-leave-to { opacity: 0; }
.modal-enter-from .modal,
.modal-leave-to .modal { transform: scale(0.96) translateY(8px); opacity: 0; }
</style>
