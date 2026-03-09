<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Cpu, Activity, GitBranch, RefreshCw,
  HardDrive, Clock, Database, Globe, BarChart2, Zap,
} from 'lucide-vue-next'
import RegisterServerModal from '../components/RegisterServerModal.vue'
import { version } from '../../package.json'

const router = useRouter()

const connectUrl  = ref('')
const showModal   = ref(false)
const urlError    = ref('')

function handleConnect() {
  const raw = connectUrl.value.trim()
  if (!raw) { urlError.value = 'Please enter a URL.'; return }
  try {
    new URL(raw)
    urlError.value = ''
    showModal.value = true
  } catch {
    urlError.value = 'Please enter a valid URL (e.g. http://localhost:8080/actuator).'
  }
}

function onRegistered() {
  router.push('/dashboard')
}

const METRICS = [
  {
    icon: Cpu,
    title: 'CPU Usage',
    desc: 'Measures real time CPU consumption',
  },
  {
    icon: Activity,
    title: 'System Load',
    desc: 'Tracks the 1-minute OS load average to reveal how saturated the host machine is beyond just the JVM itself.',
  },
  {
    icon: GitBranch,
    title: 'JVM Threads',
    desc: 'Counts live JVM threads to catch thread leaks or exhausted thread pools before they cause outages.',
  },
  {
    icon: RefreshCw,
    title: 'GC Overhead',
    desc: 'Reports the percentage of time spent in garbage collection.',
  },
  {
    icon: HardDrive,
    title: 'Disk Space',
    desc: 'Compares used to total disk capacity so you catch storage issues early.',
  },
  {
    icon: Clock,
    title: 'Uptime',
    desc: 'Shows time total time Server has been Up',
  },
  {
    icon: Database,
    title: 'JVM Heap Memory',
    desc: 'Tracks how much JVM is actively using, making memory leaks visible before they become fatal.',
  },
  {
    icon: Globe,
    title: 'Total Requests',
    desc: 'Counts all HTTP requests served so you can detect sudden drops or unexpected spikes.',
  },
  {
    icon: BarChart2,
    title: 'HTTP Status Codes',
    desc: 'Get real Timee over view of all http status, 2xx success, 3xx redirects, 4xx client errors and 5xx server errors.',
  },
  {
    icon: Zap,
    title: 'Response Time',
    desc: 'Records the average HTTP response latency in milliseconds per poll cycle so you can track performance trends over time.',
  },
]
</script>

<template>
  <div class="about">

    <!-- Hero -->
    <div class="hero">
      <h1 class="hero-title">
        Spring Actuator<br />
        <span class="hero-accent">Monitor UI</span>
      </h1>
      <p class="hero-desc">
        Paste your Spring Boot Actuator URL and get instant live metrics.

        CPU, memory, HTTP, JVM health.
      </p>
    </div>

    <!-- Register section -->
    <div class="register-card">
      <div class="register-row">
        <input
          v-model="connectUrl"
          class="url-input"
          type="text"
          placeholder="http://localhost:8080/actuator"
          @keydown.enter="handleConnect"
        />
        <button class="connect-btn" @click="handleConnect">Connect</button>
      </div>
      <div v-if="urlError" class="url-error">{{ urlError }}</div>
    </div>

    <!-- What's monitored -->
    <div class="section-head">WHAT'S MONITORED</div>

    <div class="metrics-grid">
      <div v-for="m in METRICS" :key="m.title" class="metric-card">
        <div class="metric-icon">
          <component :is="m.icon" :size="20" />
        </div>
        <div class="metric-title">{{ m.title }}</div>
        <p class="metric-desc">{{ m.desc }}</p>
      </div>
    </div>

    <!-- Footer -->
    <p class="about-footer">Monitor UI v{{ version }}</p>

  </div>

  <RegisterServerModal
    v-model="showModal"
    :prefillUrl="connectUrl"
    @registered="onRegistered"
  />
</template>

<style scoped>
.about {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 28px;
  padding: 28px 5% 0;
  min-height: calc(100vh - 48px); /* 48px = topbar */
  width: 100%;
  box-sizing: border-box;
}

/* Hero */
.hero {
  text-align: center;
  max-width: 550px;
}

.hero-title {
  font-size: clamp(32px, 4.5vw, 52px);
  font-weight: 800;
  line-height: 1.12;
  letter-spacing: -0.025em;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.hero-accent {
  background: linear-gradient(135deg, hsl(270,70%,55%) 0%, hsl(300,65%,58%) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-style: italic;
}

.hero-desc {
  font-size: 18px;
  color: var(--text-muted);
  line-height: 1.6;
}

/* Register card */
.register-card {
  width: 100%;
  max-width: 540px;
  display: flex;
  flex-direction: column;
  gap: 17px;
}
.register-row {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.url-input {
  width: 100%;
  padding: 13px 15px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--surface);
  color: var(--text-primary);
  font-size: 15px;
  font-family: inherit;
  outline: none;
  transition: border-color 0.15s;
}
.url-input:focus        { border-color: var(--accent); }
.url-input::placeholder { color: var(--text-muted); }

.connect-btn {
  width: 100%;
  padding: 14px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 700;
  color: hsl(0, 0%, 100%);
  background: hsl(162, 60%, 38%);
  transition: opacity 0.15s;
  letter-spacing: 0.03em;
}
.connect-btn:hover { opacity: 0.88; }

.url-error {
  font-size: 13px;
  color: hsl(0, 65%, 52%);
}


.section-head {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--text-muted);
  align-self: flex-start;
  width: 100%;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 14px;
  width: 100%;
}
@media (max-width: 1000px) { .metrics-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 640px)  { .metrics-grid { grid-template-columns: 1fr; } }

.metric-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 18px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
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
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 2.1;
}

.metric-desc {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.7;
  margin: 0;
}

.about-footer {
  margin-top: auto;
  padding: 12px 0 10px;
  font-size: 12px;
  color: var(--text-muted);
}

/* Mobile*/
@media (max-width: 640px) {
  .about        { gap: 22px; padding-top: 20px; }
  .hero-desc    { font-size: 15px; }
  .connect-btn  { font-size: 15px; padding: 13px; }
  .metric-card  { padding: 16px 14px; }
  .metric-title { font-size: 15px; }
  .metric-desc  { font-size: 13px; }
}
</style>
