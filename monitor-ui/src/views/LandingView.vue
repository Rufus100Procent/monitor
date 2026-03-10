<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  Cpu, Activity, GitBranch, RefreshCw,
  HardDrive, Clock, Database, Globe, BarChart2, Zap,
} from 'lucide-vue-next'
import { useAuth } from '../composables/useAuth'
import { loginUser, registerUser } from '../api/auth'
import { version } from '../../package.json'

const router = useRouter()
const route  = useRoute()
const { setToken } = useAuth()

// Driven by the URL — no local state needed
const authMode = computed<null | 'login' | 'register'>(() => {
  if (route.path === '/login')    return 'login'
  if (route.path === '/register') return 'register'
  return null
})

const username = ref('')
const password = ref('')
const error    = ref('')
const success  = ref('')
const loading  = ref(false)

// Clear messages when switching routes
watch(authMode, () => { error.value = ''; success.value = '' })

async function handleSubmit() {
  error.value   = ''
  success.value = ''
  if (!username.value.trim() || !password.value.trim()) {
    error.value = 'Username and password are required.'
    return
  }
  loading.value = true
  try {
    if (authMode.value === 'login') {
      const res = await loginUser(username.value.trim(), password.value)
      setToken(res.accessToken)
      router.push('/dashboard')
    } else {
      await registerUser(username.value.trim(), password.value)
      success.value = 'Account created! You can now log in.'
      password.value = ''
      router.push('/login')
    }
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Something went wrong'
  } finally {
    loading.value = false
  }
}

function switchMode(mode: 'login' | 'register') {
  router.push(`/${mode}`)
}

const METRICS = [
  { icon: Cpu,       title: 'CPU Usage',        desc: 'Measures real-time CPU consumption across the JVM process.' },
  { icon: Activity,  title: 'System Load',       desc: 'Tracks the 1-minute OS load average beyond just the JVM itself.' },
  { icon: GitBranch, title: 'JVM Threads',       desc: 'Counts live JVM threads to catch leaks before they cause outages.' },
  { icon: RefreshCw, title: 'GC Overhead',       desc: 'Reports the percentage of time spent in garbage collection.' },
  { icon: HardDrive, title: 'Disk Space',        desc: 'Compares used to total disk capacity so you catch storage issues early.' },
  { icon: Clock,     title: 'Uptime',            desc: 'Shows total time the server has been continuously running.' },
  { icon: Database,  title: 'JVM Heap Memory',   desc: 'Tracks active heap usage, making memory leaks visible early.' },
  { icon: Globe,     title: 'Total Requests',    desc: 'Counts all HTTP requests to detect drops or unexpected spikes.' },
  { icon: BarChart2, title: 'HTTP Status Codes', desc: 'Real-time breakdown of 2xx, 3xx, 4xx and 5xx responses.' },
  { icon: Zap,       title: 'Response Time',     desc: 'Average HTTP latency in milliseconds per poll cycle.' },
]
</script>

<template>
  <div class="landing-shell">

    <!-- Top bar -->
    <header class="landing-bar">
      <button class="bar-brand" @click="router.push('/')">Monitor UI</button>
      <nav class="bar-actions">
        <button class="btn-docs" @click="router.push('/docs')">Docs</button>
      </nav>
    </header>

    <!-- Auth form  -->
    <div v-if="authMode" class="auth-page">
      <div class="auth-card">
        <h2 class="auth-title">{{ authMode === 'login' ? 'Sign in' : 'Create account' }}</h2>

        <div v-if="success" class="auth-success">{{ success }}</div>
        <div v-if="error" class="auth-error">{{ error }}</div>

        <form class="auth-form" @submit.prevent="handleSubmit">
          <label class="auth-label">
            Username
            <input
              v-model="username"
              class="auth-input"
              type="text"
              autocomplete="username"
              placeholder="your-username"
              required
            />
          </label>

          <label class="auth-label">
            Password
            <input
              v-model="password"
              class="auth-input"
              type="password"
              autocomplete="current-password"
              placeholder="••••••••"
              required
            />
          </label>

          <button class="auth-submit" type="submit" :disabled="loading">
            {{ loading ? 'Please wait…' : (authMode === 'login' ? 'Sign in' : 'Create account') }}
          </button>
        </form>

        <p class="auth-switch">
          <span v-if="authMode === 'login'">
            No account?
            <button class="auth-switch-btn" @click="switchMode('register')">Register</button>
          </span>
          <span v-else>
            Already have an account?
            <button class="auth-switch-btn" @click="switchMode('login')">Sign in</button>
          </span>
        </p>
      </div>
    </div>

    <!-- Landing content -->
    <template v-else>

      <!-- Hero band -->
      <section class="hero-band">
        <div class="hero">
          <p class="hero-eyebrow">Spring Boot · Real-time metrics</p>
          <h1 class="hero-title">
            Spring Actuator<br />
            <span class="hero-accent">Monitor UI</span>
          </h1>
          <p class="hero-desc">
            Connect your Spring Boot Actuator endpoint and get instant live metrics —
            CPU, memory, HTTP, JVM health, and more.
          </p>
          <div class="hero-cta">
            <button class="btn-primary" @click="switchMode('register')">Get started free</button>
            <button class="btn-ghost btn-ghost--border" @click="switchMode('login')">Sign in</button>
          </div>
        </div>
      </section>

      <!-- Metrics section -->
      <section class="metrics-section">
        <div class="container">
          <div class="section-eyebrow">What's monitored</div>
          <h2 class="section-title">Everything you need to watch</h2>
          <p class="section-sub">
            Ten live metrics polled directly from your actuator endpoint.
          </p>

          <div class="metrics-grid">
            <div v-for="m in METRICS" :key="m.title" class="metric-card">
              <div class="metric-icon">
                <component :is="m.icon" :size="18" />
              </div>
              <div class="metric-title">{{ m.title }}</div>
              <p class="metric-desc">{{ m.desc }}</p>
            </div>
          </div>
        </div>
      </section>

      <footer class="landing-footer">
        Monitor UI v{{ version }}
      </footer>

    </template>
  </div>
</template>

<style scoped>
/* Shell─ */
.landing-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg);
}

/* Container─ */
.container {
  width: 100%;
  max-width: 1160px;
  margin: 0 auto;
  padding: 0 32px;
  box-sizing: border-box;
}

/* Top bar─ */
.landing-bar {
  height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  background: var(--surface);
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
  position: sticky;
  top: 0;
  z-index: 10;
}

.bar-brand {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.01em;
  background: linear-gradient(135deg, hsl(270,70%,55%) 0%, hsl(300,65%,58%) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  cursor: pointer;
  transition: opacity 0.15s;
}

.bar-brand:hover { opacity: 0.8; }

.bar-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* Shared button primitives */
.btn-docs {
  padding: 6px 13px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  background: var(--surface);
  color: var(--text-primary);
  border: 1px solid var(--border);
  cursor: pointer;
  transition: background 0.12s;
}

.btn-docs:hover { background: var(--nav-hover); }

.btn-ghost {
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  background: transparent;
  transition: background 0.14s, color 0.14s;
}

.btn-ghost:hover,
.btn-ghost--active {
  background: var(--nav-hover);
  color: var(--text-primary);
}

.btn-ghost--border {
  border: 1px solid var(--border);
}

.btn-ghost--border:hover {
  border-color: transparent;
}

.btn-primary {
  padding: 7px 18px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 700;
  color: hsl(0, 0%, 100%);
  background: hsl(162, 65%, 28%);
  transition: opacity 0.15s;
}

.btn-primary:hover        { opacity: 0.88; }
.btn-primary--muted       { opacity: 0.5; }

/* Hero band─ */
.hero-band {
  background: linear-gradient(
    150deg,
    hsl(270, 45%, 97%) 0%,
    hsl(300, 30%, 97%) 55%,
    hsl(270, 45%, 97%) 100%
  );
  border-bottom: 1px solid var(--border);
  padding: 80px 32px 92px;
}

:root.dark .hero-band {
  background: linear-gradient(
    150deg,
    hsl(270, 20%, 12%) 0%,
    hsl(300, 15%, 12%) 55%,
    hsl(270, 20%, 12%) 100%
  );
}

.hero {
  text-align: center;
  max-width: 660px;
  margin: 0 auto;
}

.hero-eyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: hsl(162, 60%, 38%);
  margin-bottom: 18px;
}

.hero-title {
  font-size: clamp(50px, 15vw, 70px);
  font-weight: 800;
  line-height: 1.1;
  letter-spacing: -0.03em;
  color: var(--text-primary);
  margin-bottom: 20px;
}

.hero-accent {
  background: linear-gradient(135deg, hsl(270,70%,52%) 0%, hsl(300,65%,55%) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-style: italic;
}

.hero-desc {
  font-size: 17px;
  color: var(--text-muted);
  line-height: 1.7;
  margin-bottom: 36px;
  max-width: 460px;
  margin-left: auto;
  margin-right: auto;
}

.hero-cta {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}

.hero-cta .btn-primary {
  padding: 11px 26px;
  font-size: 14px;
  border-radius: 10px;
}

.hero-cta .btn-ghost {
  padding: 10px 22px;
  font-size: 14px;
  border-radius: 10px;
}

/* Metrics section─ */
.metrics-section {
  padding: 68px 0 72px;
}

.section-eyebrow {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: hsl(162, 60%, 38%);
  margin-bottom: 10px;
}

.section-title {
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.section-sub {
  font-size: 15px;
  color: var(--text-muted);
  line-height: 1.65;
  margin-bottom: 36px;
  max-width: 480px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.metric-card {
  background: var(--surface);
  border: 1.5px solid var(--border);
  border-radius: 14px;
  padding: 22px 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
}

.metric-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 28px hsl(220, 20%, 12%, 0.09);
  border-color: hsl(270, 55%, 78%);
}

:root.dark .metric-card:hover {
  box-shadow: 0 8px 28px hsl(220, 20%, 5%, 0.35);
  border-color: hsl(270, 40%, 40%);
}

.metric-icon {
  width: 36px;
  height: 36px;
  border-radius: 9px;
  background: hsla(270, 70%, 58%, 0.1);
  color: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.metric-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.3;
}

.metric-desc {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.65;
  margin: 0;
}

/* Auth page─ */
.auth-page {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  background: var(--bg);
}

.auth-card {
  width: 100%;
  max-width: 380px;
  background: var(--surface);
  border: 1.5px solid var(--border);
  border-radius: 16px;
  padding: 36px 32px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.auth-title {
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--text-primary);
  margin: 0;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.auth-label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}

.auth-input {
  padding: 10px 13px;
  border: 1.5px solid var(--border);
  border-radius: 9px;
  background: var(--bg);
  color: var(--text-primary);
  font-size: 14px;
  font-family: inherit;
  outline: none;
  transition: border-color 0.15s;
}

.auth-input:focus        { border-color: var(--accent); }
.auth-input::placeholder { color: var(--text-muted); }

.auth-submit {
  margin-top: 4px;
  padding: 11px;
  border-radius: 9px;
  font-size: 14px;
  font-weight: 700;
  color: hsl(0, 0%, 100%);
  background: hsl(162, 65%, 28%);
  transition: opacity 0.15s;
}

.auth-submit:hover    { opacity: 0.88; }
.auth-submit:disabled { opacity: 0.5; cursor: not-allowed; }

.auth-error {
  font-size: 13px;
  color: hsl(0, 70%, 38%);
  background: hsl(0, 80%, 97%);
  border: 1px solid hsl(0, 65%, 88%);
  border-radius: 8px;
  padding: 9px 12px;
}

.auth-success {
  font-size: 13px;
  color: hsl(142, 55%, 32%);
  background: hsl(142, 60%, 96%);
  border: 1px solid hsl(142, 50%, 82%);
  border-radius: 8px;
  padding: 9px 12px;
}

:root.dark .auth-error {
  background: hsl(0, 40%, 16%);
  border-color: hsl(0, 45%, 28%);
  color: hsl(0, 65%, 65%);
}

:root.dark .auth-success {
  background: hsl(142, 30%, 14%);
  border-color: hsl(142, 35%, 24%);
  color: hsl(142, 50%, 55%);
}

.auth-switch {
  font-size: 13px;
  color: var(--text-muted);
  text-align: center;
  margin: 0;
}

.auth-switch-btn {
  color: var(--accent);
  font-weight: 600;
  text-decoration: underline;
  background: none;
}

/* Footer */
.landing-footer {
  padding: 18px 32px;
  font-size: 12px;
  color: var(--text-muted);
  border-top: 1px solid var(--border);
  text-align: center;
  margin-top: auto;
}

/* Responsive */
@media (max-width: 640px) {
  .landing-bar  { padding: 0 18px; }
  .hero-band    { padding: 56px 20px 64px; }
  .hero-title   { font-size: 36px; }
  .hero-desc    { font-size: 15px; }
  .metrics-section { padding: 48px 0 52px; }
  .container    { padding: 0 18px; }
  .metrics-grid { grid-template-columns: 1fr 1fr; gap: 12px; }
  .auth-card    { padding: 28px 22px; }
}

@media (max-width: 420px) {
  .metrics-grid { grid-template-columns: 1fr; }
}
</style>
