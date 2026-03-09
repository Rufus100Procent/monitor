<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { LoaderCircle } from 'lucide-vue-next'
import { getSnapshots, getLatestSnapshot } from '../../api/servers'
import type { Snapshot, SnapshotPage } from '../../api/servers'
import { useServers } from '../../composables/useServers'
import SnapshotChartView from './SnapshotChartView.vue'
import SnapshotTableView from './SnapshotTableView.vue'

const route = useRoute()
const { servers, ensureLoaded } = useServers()
const serverId = computed(() => route.params.id as string)
const server   = computed(() => servers.value.find(s => s.id === serverId.value))

onMounted(ensureLoaded)

// Snapshot storage
const liveSnapshots       = ref<Snapshot[]>([])
const historicalSnapshots = ref<Snapshot[]>([])
const loading             = ref(false)
const fetchError          = ref('')
const connectionDown      = ref(false)

// Pagination state (table mode only)
const tablePage  = ref(0)
const tableSize  = 50
const tableMeta  = ref<Pick<SnapshotPage, 'totalRows' | 'totalPages' | 'hasNext' | 'hasPrevious'> | null>(null)

// Historical date range (stored so page changes can re-fetch)
let historicalFrom: Date | null = null
let historicalTo:   Date | null = null

const displaySnapshots = computed(() => {
  if (connectionDown.value) return []
  return selectedRange.value === 'live' ? liveSnapshots.value : historicalSnapshots.value
})
const latest = computed(() => liveSnapshots.value[liveSnapshots.value.length - 1] ?? null)

// Live polling (/latest every 2 s)
let liveTimer: ReturnType<typeof setInterval> | null = null

async function seedLiveFromList() {
  loading.value = true
  try {
    const page = await getSnapshots(serverId.value, { size: 50 })
    liveSnapshots.value = [...page.data].reverse()
  } catch { /* non-fatal */ } finally {
    loading.value = false
  }
}

async function fetchLatestAndAppend() {
  if (!serverId.value) return
  try {
    const snap = await getLatestSnapshot(serverId.value)
    connectionDown.value = false
    const last = liveSnapshots.value[liveSnapshots.value.length - 1]
    if (!last || last.id !== snap.id) {
      liveSnapshots.value = [...liveSnapshots.value.slice(-99), snap]
    }
  } catch {
    connectionDown.value = true
  }
}

function startLive() {
  stopLive()
  liveSnapshots.value = []
  connectionDown.value = false
  seedLiveFromList()
  liveTimer = setInterval(fetchLatestAndAppend, 2_000)
}

function stopLive() {
  if (liveTimer) { clearInterval(liveTimer); liveTimer = null }
}

//  Historical fetch (paginated)
async function loadHistorical(from: Date, to: Date, page = 0) {
  loading.value = true
  fetchError.value = ''
  historicalSnapshots.value = []
  historicalFrom = from
  historicalTo   = to
  tablePage.value = page
  try {
    const result = await getSnapshots(serverId.value, {
      from: from.toISOString(), to: to.toISOString(),
      page, size: tableSize,
    })
    historicalSnapshots.value = result.data
    tableMeta.value = {
      totalRows:   result.totalRows,
      totalPages:  result.totalPages,
      hasNext:     result.hasNext,
      hasPrevious: result.hasPrevious,
    }
  } catch (e) {
    fetchError.value = e instanceof Error ? e.message : 'Failed to load snapshots.'
    tableMeta.value = null
  } finally {
    loading.value = false
  }
}

function changeTablePage(newPage: number) {
  if (historicalFrom && historicalTo) loadHistorical(historicalFrom, historicalTo, newPage)
}

//  Filter state
type RangeKey = 'live' | '1h' | '7d' | '30d' | 'custom'

const RANGE_BUTTONS: { key: RangeKey; label: string }[] = [
  { key: 'live',   label: 'Live'   },
  { key: '1h',     label: '1H'     },
  { key: '7d',     label: '7D'     },
  { key: '30d',    label: '30D'    },
  { key: 'custom', label: 'Custom' },
]

const selectedRange   = ref<RangeKey>('live')
const showCustom      = ref(false)
const customSearched  = ref(false)

const _now = new Date()
const customFrom = ref({ year: _now.getUTCFullYear(), month: _now.getUTCMonth() + 1, day: _now.getUTCDate() })
const customTo   = ref({ year: _now.getUTCFullYear(), month: _now.getUTCMonth() + 1, day: _now.getUTCDate() })

watch(() => [customFrom.value.year, customFrom.value.month], () => {
  const max = daysInMonth(customFrom.value.year, customFrom.value.month)
  if (customFrom.value.day > max) customFrom.value.day = max
})
watch(() => [customTo.value.year, customTo.value.month], () => {
  const max = daysInMonth(customTo.value.year, customTo.value.month)
  if (customTo.value.day > max) customTo.value.day = max
})

function applyFilter(key: RangeKey) {
  stopLive()
  selectedRange.value = key
  showCustom.value = false
  connectionDown.value = false
  tablePage.value = 0
  tableMeta.value = null

  if (key === 'live') { startLive(); return }
  if (key === 'custom') { showCustom.value = true; return }

  const to     = new Date()
  const msBack = key === '1h' ? 3_600_000 : key === '7d' ? 7 * 86_400_000 : 30 * 86_400_000
  const from   = new Date(to.getTime() - msBack)
  loadHistorical(from, to)
}

function applyCustomSearch() {
  const from = new Date(Date.UTC(customFrom.value.year, customFrom.value.month - 1, customFrom.value.day,  0,  0,  0))
  const to   = new Date(Date.UTC(customTo.value.year,   customTo.value.month - 1,   customTo.value.day,  23, 59, 59))
  customSearched.value = true
  tablePage.value = 0
  loadHistorical(from, to)
}

onMounted(() => startLive())
onBeforeUnmount(() => stopLive())

watch(serverId, () => {
  selectedRange.value  = 'live'
  showCustom.value     = false
  customSearched.value = false
  tablePage.value      = 0
  tableMeta.value      = null
  historicalFrom       = null
  historicalTo         = null
  startLive()
})

//  Toggle: Live, charts, historical, table
const isTableMode = computed(() => selectedRange.value !== 'live')

//  Stats from latest snapshot
function fmtBytes(bytes: number): string {
  if (!bytes) return '—'
  if (bytes >= 1024 ** 4) return `${(bytes / 1024 ** 4).toFixed(1)} TB`
  if (bytes >= 1024 ** 3) return `${(bytes / 1024 ** 3).toFixed(1)} GB`
  return `${Math.round(bytes / 1024 ** 2)} MB`
}

function fmtUptime(s: number): string {
  if (!s) return '—'
  const d = Math.floor(s / 86400), h = Math.floor((s % 86400) / 3600), m = Math.floor((s % 3600) / 60)
  if (d > 0) return `${d}d ${h}h ${m}m`
  if (h > 0) return `${h}h ${m}m`
  return `${m}m`
}

const diskUsed = computed(() =>
  latest.value ? Math.max(0, latest.value.diskTotalBytes - latest.value.diskFreeBytes) : 0
)

const stats = computed(() => {
  const s = latest.value
  if (!s) return null
  const gcPct = s.gcOverhead * 100
  const dPct  = s.diskTotalBytes ? Math.round((diskUsed.value / s.diskTotalBytes) * 100) : 0
  return {
    sysLoad:   s.systemLoad.toFixed(2),
    jvmThread: s.jvmThreadsLive,
    gcOverhead: gcPct < 0.01 ? '<0.01%' : `${gcPct.toFixed(2)}%`,
    gcPct,
    diskUsed:  fmtBytes(diskUsed.value),
    diskTotal: fmtBytes(s.diskTotalBytes),
    diskPct:   dPct,
    uptime:    fmtUptime(s.uptimeSeconds),
  }
})

const gcClass = computed(() => {
  const p = stats.value?.gcPct ?? 0
  return p > 5 ? 'val-danger' : p > 2 ? 'val-warning' : 'val-good'
})

//  Custom date picker helpers
const MONTHS = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec']
const selectYears = computed(() => {
  const arr: number[] = []
  for (let y = 2024; y <= new Date().getUTCFullYear() + 1; y++) arr.push(y)
  return arr
})
function daysInMonth(year: number, month: number) {
  return new Date(Date.UTC(year, month, 0)).getUTCDate()
}

</script>

<template>
  <div class="basics">

    <div v-if="!server" class="state-box">
      <LoaderCircle :size="22" class="spin" />
    </div>

    <template v-else>

      <!--  Header  -->
      <div class="page-header">
        <div class="header-left">
          <h1 class="server-name">{{ server.appName }}</h1>
          <div v-if="server.appVersion" class="server-version">v{{ server.appVersion }}</div>
        </div>

        <div class="header-right">
          <div class="range-buttons">
            <button
              v-for="btn in RANGE_BUTTONS"
              :key="btn.key"
              class="range-btn"
              :class="{ active: selectedRange === btn.key }"
              @click="applyFilter(btn.key)"
            >
              <span v-if="btn.key === 'live'" class="live-dot" :class="{ pulsing: selectedRange === 'live' }" />
              {{ btn.label }}
            </button>
          </div>

          <!-- Custom date range accordion -->
          <div class="custom-wrap" :class="{ open: showCustom }">
            <div class="custom-inner">
              <div class="custom-bar">
                <div class="custom-group">
                  <span class="custom-label">From</span>
                  <select v-model="customFrom.year">
                    <option v-for="y in selectYears" :key="y" :value="y">{{ y }}</option>
                  </select>
                  <select v-model="customFrom.month">
                    <option v-for="(m, i) in MONTHS" :key="i" :value="i + 1">{{ m }}</option>
                  </select>
                  <select v-model="customFrom.day">
                    <option v-for="d in daysInMonth(customFrom.year, customFrom.month)" :key="d" :value="d">
                      {{ String(d).padStart(2, '0') }}
                    </option>
                  </select>
                </div>

                <div class="custom-group">
                  <span class="custom-label">To</span>
                  <select v-model="customTo.year">
                    <option v-for="y in selectYears" :key="y" :value="y">{{ y }}</option>
                  </select>
                  <select v-model="customTo.month">
                    <option v-for="(m, i) in MONTHS" :key="i" :value="i + 1">{{ m }}</option>
                  </select>
                  <select v-model="customTo.day">
                    <option v-for="d in daysInMonth(customTo.year, customTo.month)" :key="d" :value="d">
                      {{ String(d).padStart(2, '0') }}
                    </option>
                  </select>
                </div>

                <button class="search-btn" @click="applyCustomSearch">Search</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!--  Server down banner  -->
      <div v-if="connectionDown" class="down-banner">
        <span class="down-icon">●</span>
        Could not connect to the server
      </div>

      <!--Inline status (loading / error / custom hint) -->
      <div v-if="loading" class="status-bar">
        <LoaderCircle :size="14" class="spin" /> Loading snapshots…
      </div>
      <div v-else-if="fetchError" class="error-box">{{ fetchError }}</div>
      <div v-else-if="selectedRange === 'custom' && !customSearched" class="status-bar">
        Select a date range above and click Search.
      </div>

      <!--  Stats row (always visible; dashes in table mode)  -->
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-label">System Load</div>
          <div class="stat-val">{{ isTableMode ? '—' : (stats?.sysLoad ?? '—') }}</div>
          <div class="stat-sub">1-min load avg</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">JVM Threads</div>
          <div class="stat-val">
            {{ isTableMode ? '—' : (stats?.jvmThread ?? '—') }}
            <span v-if="!isTableMode && stats" class="stat-unit">live</span>
          </div>
          <div class="stat-sub">currently active</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">GC Overhead</div>
          <div class="stat-val" :class="!isTableMode && stats ? gcClass : ''">
            {{ isTableMode ? '—' : (stats?.gcOverhead ?? '—') }}
          </div>
          <div class="stat-sub">% time in GC</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Disk</div>
          <div class="stat-val disk-val">
            {{ isTableMode ? '—' : (stats?.diskUsed ?? '—') }}
            <template v-if="!isTableMode && stats">
              <span class="disk-sep">/</span>
              <span class="disk-total">{{ stats.diskTotal }}</span>
            </template>
          </div>
          <div class="disk-track">
            <div class="disk-fill" :style="{ width: (!isTableMode ? (stats?.diskPct ?? 0) : 0) + '%' }" />
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Uptime</div>
          <div class="stat-val">{{ isTableMode ? '—' : (stats?.uptime ?? '—') }}</div>
          <div class="stat-sub">since last restart</div>
        </div>
      </div>

      <!--  Charts (live) or Table (historical)  -->
      <SnapshotChartView
        v-if="!isTableMode"
        :snapshots="displaySnapshots"
        :selectedRange="selectedRange"
      />
      <SnapshotTableView
        v-else
        :snapshots="displaySnapshots"
        :selectedRange="selectedRange"
        :page="tablePage"
        :totalPages="tableMeta?.totalPages ?? 0"
        :totalRows="tableMeta?.totalRows ?? 0"
        :hasNext="tableMeta?.hasNext ?? false"
        :hasPrevious="tableMeta?.hasPrevious ?? false"
        @page-change="changeTablePage"
      />

    </template>
  </div>
</template>

<style scoped>
.basics {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/*  Server down banner  */
.down-banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px 20px;
  border-radius: 10px;
  background: hsl(0, 65%, 97%);
  border: 1px solid hsl(0, 65%, 85%);
  color: hsl(0, 65%, 48%);
  font-size: 14px;
  font-weight: 600;
  text-align: center;
}
:root.dark .down-banner {
  background: hsl(0, 40%, 13%);
  border-color: hsl(0, 40%, 24%);
  color: hsl(0, 65%, 62%);
}
.down-icon {
  font-size: 9px;
  animation: livepulse 1.4s ease-in-out infinite;
}

/*  Inline status bar  */
.status-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-muted);
  padding: 6px 2px;
}

/*  State boxes  */
.state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  height: 200px;
  font-size: 13px;
  color: var(--text-muted);
}

.error-box {
  font-size: 13px;
  color: hsl(0, 65%, 50%);
  background: hsl(0, 65%, 97%);
  border: 1px solid hsl(0, 65%, 88%);
  border-radius: 8px;
  padding: 10px 14px;
}
:root.dark .error-box {
  background: hsl(0, 40%, 14%);
  border-color: hsl(0, 40%, 24%);
  color: hsl(0, 65%, 65%);
}

.spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/*  Page header  */
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.server-name {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.01em;
}

.server-version {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 3px;
}

.header-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

/*  Range buttons  */
.range-buttons {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

@media (max-width: 600px) {
  .page-header    { flex-direction: column; gap: 10px; }
  .header-right   { align-items: flex-start; width: 100%; }
  .range-buttons  { justify-content: flex-start; }
  .custom-wrap    { width: 100%; }
}

.range-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 600;
  padding: 5px 12px;
  border-radius: 6px;
  border: 1px solid var(--border);
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.13s;
}
.range-btn:hover  { border-color: var(--accent); color: var(--accent); }
.range-btn.active { background: hsla(270,70%,58%,0.1); border-color: var(--accent); color: var(--accent); }

/*  Live dot  */
.live-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: hsl(220, 10%, 70%);
  flex-shrink: 0;
  transition: background 0.2s;
}
.live-dot.pulsing {
  background: hsl(0, 72%, 55%);
  animation: livepulse 1.4s ease-in-out infinite;
}
@keyframes livepulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50%       { transform: scale(1.55); opacity: 0.55; }
}

/*  Custom date accordion  */
.custom-wrap {
  display: grid;
  grid-template-rows: 0fr;
  transition: grid-template-rows 0.26s ease;
  width: 100%;
}
.custom-wrap.open { grid-template-rows: 1fr; }
.custom-inner     { overflow: hidden; }

.custom-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding: 10px 14px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 10px;
  margin-top: 4px;
}

.custom-group {
  display: flex;
  align-items: center;
  gap: 6px;
}

.custom-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  min-width: 22px;
}

.custom-bar select {
  padding: 5px 8px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--bg);
  color: var(--text-primary);
  font-size: 12px;
  font-family: inherit;
  cursor: pointer;
  outline: none;
}
.custom-bar select:focus { border-color: var(--accent); }

.search-btn {
  padding: 5px 14px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  background: var(--accent);
  color: hsl(0, 0%, 100%);
  transition: opacity 0.15s;
  margin-left: 4px;
}
.search-btn:hover { opacity: 0.88; }

/*  Stats row  */
.stats-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
}
@media (max-width: 1100px) { .stats-row { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 640px)  { .stats-row { grid-template-columns: repeat(2, 1fr); } }

.stat-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 14px 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-label {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.07em;
  text-transform: uppercase;
  color: var(--text-muted);
  margin-bottom: 6px;
}

.stat-val {
  font-size: 20px;
  font-weight: 800;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
  margin-bottom: 6px;
  word-break: break-word;
}

@media (max-width: 640px) {
  .stat-val  { font-size: 16px; }
  .stat-card { padding: 10px 12px 8px; }
}
.stat-unit { font-size: 12px; font-weight: 600; opacity: 0.7; }
.stat-sub  { font-size: 11px; color: var(--text-muted); }

.val-good    { color: hsl(142, 65%, 40%) !important; }
.val-warning { color: hsl(38,  90%, 48%) !important; }
.val-danger  { color: hsl(0,   65%, 52%) !important; }

.disk-val   { display: flex; align-items: baseline; gap: 4px; }
.disk-sep   { opacity: 0.35; font-weight: 400; font-size: 14px; }
.disk-total { font-size: 13px; font-weight: 600; opacity: 0.55; }

.disk-track {
  height: 3px;
  background: var(--border);
  border-radius: 2px;
  overflow: hidden;
  margin-top: 2px;
}
.disk-fill {
  height: 100%;
  border-radius: 2px;
  background: hsl(162, 65%, 42%);
  transition: width 0.4s ease;
}

</style>
