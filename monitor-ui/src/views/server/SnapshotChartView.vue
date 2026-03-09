<script setup lang="ts">
import { computed } from 'vue'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Tooltip,
  Filler,
} from 'chart.js'
import { Line } from 'vue-chartjs'
import type { Snapshot } from '../../api/servers'
import { useTheme } from '../../composables/useTheme'
import { useDisplayTimezone } from '../../composables/useDisplayTimezone'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Filler)

const props = defineProps<{
  snapshots: Snapshot[]
  selectedRange: string
}>()

const { dark } = useTheme()
const { selectedIana } = useDisplayTimezone()

// Chart labels
const chartCtx = computed(() => {
  const tz    = selectedIana.value
  const range = props.selectedRange

  const labels = props.snapshots.map(s => {
    const d = new Date(s.polledAt)
    if (range === 'live' || range === '1h') {
      return d.toLocaleTimeString('en-GB', { timeZone: tz, hour: '2-digit', minute: '2-digit', second: '2-digit' })
    }
    if (range === '7d') {
      return d.toLocaleDateString('en-GB', { timeZone: tz, weekday: 'short', day: 'numeric', month: 'short' }) +
             ' ' + d.toLocaleTimeString('en-GB', { timeZone: tz, hour: '2-digit', minute: '2-digit' })
    }
    return d.toLocaleDateString('en-GB', { timeZone: tz, month: 'short', day: 'numeric' }) +
           ' ' + d.toLocaleTimeString('en-GB', { timeZone: tz, hour: '2-digit', minute: '2-digit' })
  })

  return { labels }
})

function dataset(getValue: (s: Snapshot) => number): number[] {
  return props.snapshots.map(s => parseFloat(getValue(s).toFixed(4)))
}

// Chart options
function makeLineOpts(yTickFn: (v: number | string) => string) {
  return computed(() => ({
    responsive: true,
    maintainAspectRatio: false,
    animation: { duration: 200 } as const,
    interaction: { mode: 'index' as const, intersect: false },
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: dark.value ? 'hsl(220,15%,14%)' : 'hsl(0,0%,100%)',
        titleColor:      dark.value ? 'hsl(220,10%,85%)' : 'hsl(220,20%,15%)',
        bodyColor:       dark.value ? 'hsl(220,10%,65%)' : 'hsl(220,15%,40%)',
        borderColor:     dark.value ? 'hsl(220,15%,28%)' : 'hsl(220,15%,88%)',
        borderWidth: 1, padding: 10,
      },
    },
    scales: {
      x: {
        ticks: { color: dark.value ? 'hsl(220,10%,55%)' : 'hsl(220,15%,48%)', maxTicksLimit: 7, maxRotation: 0 },
        grid:   { color: dark.value ? 'hsl(220,15%,22%)' : 'hsl(220,15%,88%)' },
        border: { color: dark.value ? 'hsl(220,15%,22%)' : 'hsl(220,15%,88%)' },
      },
      y: {
        min: 0,
        ticks: { color: dark.value ? 'hsl(220,10%,55%)' : 'hsl(220,15%,48%)', callback: yTickFn },
        grid:   { color: dark.value ? 'hsl(220,15%,22%)' : 'hsl(220,15%,88%)' },
        border: { color: dark.value ? 'hsl(220,15%,22%)' : 'hsl(220,15%,88%)' },
      },
    },
  }))
}

// Chart data
const cpuData = computed(() => ({
  labels: chartCtx.value.labels,
  datasets: [{
    label: 'CPU %',
    data: dataset(s => s.cpuUsage * 100),
    borderColor: 'hsl(214,72%,58%)', backgroundColor: 'hsla(214,72%,58%,0.08)',
    fill: true, borderWidth: 2, pointRadius: 0, tension: 0.4,
  }],
}))
const cpuOpts = makeLineOpts(v => `${v}%`)

const memData = computed(() => ({
  labels: chartCtx.value.labels,
  datasets: [{
    label: 'Heap GB',
    data: dataset(s => s.memoryUsedBytes / (1024 ** 3)),
    borderColor: 'hsl(268,72%,58%)', backgroundColor: 'hsla(268,72%,58%,0.08)',
    fill: true, borderWidth: 2, pointRadius: 0, tension: 0.4,
  }],
}))
const memOpts = makeLineOpts(v => `${v} GB`)

const rtData = computed(() => ({
  labels: chartCtx.value.labels,
  datasets: [{
    label: 'Avg ms',
    data: dataset(s => s.httpAvgMs),
    borderColor: 'hsl(162,72%,40%)', backgroundColor: 'hsla(162,72%,40%,0.08)',
    fill: true, borderWidth: 2, pointRadius: 0, tension: 0.4,
  }],
}))
const rtOpts = makeLineOpts(v => `${v} ms`)

const chartKey = computed(() => dark.value ? 'dark' : 'light')

// HTTP stats from latest snapshot
const http = computed(() => {
  const s = props.snapshots[props.snapshots.length - 1] ?? null
  if (!s) return { total: '—', c2xx: '—', c3xx: '—', c4xx: '—', c5xx: '—', p2xx: '—', p3xx: '—', p4xx: '—', p5xx: '—' }
  const sum = s.http2xxCount + s.http3xxCount + s.http4xxCount + s.http5xxCount
  const p   = (n: number) => sum > 0 ? `${((n / sum) * 100).toFixed(1)}%` : '0.0%'
  return {
    total: s.httpRequestCount.toLocaleString(),
    c2xx: s.http2xxCount.toLocaleString(), p2xx: p(s.http2xxCount),
    c3xx: s.http3xxCount.toLocaleString(), p3xx: p(s.http3xxCount),
    c4xx: s.http4xxCount.toLocaleString(), p4xx: p(s.http4xxCount),
    c5xx: s.http5xxCount.toLocaleString(), p5xx: p(s.http5xxCount),
  }
})
</script>

<template>
  <!-- CPU + Memory -->
  <div class="charts-2col">
    <div class="chart-card">
      <div class="chart-head">
        <div class="chart-name">CPU Usage</div>
        <div class="chart-desc">system.cpu.usage</div>
      </div>
      <div class="chart-body">
        <Line :key="`cpu-${chartKey}`" :data="cpuData" :options="cpuOpts" />
      </div>
    </div>
    <div class="chart-card">
      <div class="chart-head">
        <div class="chart-name">JVM Heap Memory</div>
        <div class="chart-desc">jvm.memory.used · GB</div>
      </div>
      <div class="chart-body">
        <Line :key="`mem-${chartKey}`" :data="memData" :options="memOpts" />
      </div>
    </div>
  </div>

  <!-- HTTP stats -->
  <div class="section-label">HTTP</div>

  <div class="http-stats-row">
    <div class="http-stat">
      <div class="http-val">{{ http.total }}</div>
      <div class="http-lbl">Total Requests</div>
    </div>
    <div class="http-stat">
      <div class="http-val http-2xx">{{ http.c2xx }}</div>
      <div class="http-lbl"><span class="http-pct http-2xx">{{ http.p2xx }}</span> 2xx Success</div>
    </div>
    <div class="http-stat">
      <div class="http-val http-3xx">{{ http.c3xx }}</div>
      <div class="http-lbl"><span class="http-pct http-3xx">{{ http.p3xx }}</span> 3xx Redirect</div>
    </div>
    <div class="http-stat">
      <div class="http-val http-4xx">{{ http.c4xx }}</div>
      <div class="http-lbl"><span class="http-pct http-4xx">{{ http.p4xx }}</span> 4xx Client Errors</div>
    </div>
    <div class="http-stat">
      <div class="http-val http-5xx">{{ http.c5xx }}</div>
      <div class="http-lbl"><span class="http-pct http-5xx">{{ http.p5xx }}</span> 5xx Server Errors</div>
    </div>
  </div>

  <!-- Response Time -->
  <div class="chart-card">
    <div class="chart-head">
      <div class="chart-name">Response Time over Time</div>
      <div class="chart-desc">httpAvgMs per snapshot</div>
    </div>
    <div class="chart-body chart-body-lg">
      <Line :key="`rt-${chartKey}`" :data="rtData" :options="rtOpts" />
    </div>
  </div>
</template>

<style scoped>
.charts-2col {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
@media (max-width: 700px) { .charts-2col { grid-template-columns: 1fr; } }

.chart-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 16px;
}
.chart-head    { margin-bottom: 12px; }
.chart-name    { font-size: 13px; font-weight: 700; color: var(--text-primary); margin-bottom: 2px; }
.chart-desc    { font-size: 11px; color: var(--text-muted); }
.chart-body    { height: 160px; position: relative; overflow: hidden; }
.chart-body-lg { height: 200px; overflow: hidden; }

.section-label {
  font-size: 10px; font-weight: 700; letter-spacing: 0.1em;
  text-transform: uppercase; color: var(--text-muted);
}

.http-stats-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
}
@media (max-width: 1100px) { .http-stats-row { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 640px)  { .http-stats-row { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 380px)  { .http-stats-row { grid-template-columns: 1fr; } }

.http-stat {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 14px 16px;
}
.http-val {
  font-size: 24px; font-weight: 800; line-height: 1;
  font-variant-numeric: tabular-nums; margin-bottom: 5px;
  color: var(--text-primary);
}
@media (max-width: 640px) { .http-val { font-size: 18px; } }

.http-lbl { font-size: 12px; color: var(--text-muted); display: flex; align-items: center; gap: 4px; }
.http-pct { font-weight: 700; }
.http-2xx { color: hsl(142, 60%, 40%); }
.http-3xx { color: hsl(28,  90%, 50%); }
.http-4xx { color: hsl(48,  95%, 40%); }
.http-5xx { color: hsl(0,   65%, 52%); }
</style>
