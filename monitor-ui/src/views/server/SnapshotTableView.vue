<script setup lang="ts">
import type { Snapshot } from '../../api/api'
import { useDisplayTimezone } from '../../composables/useDisplayTimezone'

const props = withDefaults(defineProps<{
  snapshots: Snapshot[]
  selectedRange: string
  page?: number
  totalPages?: number
  totalRows?: number
  hasNext?: boolean
  hasPrevious?: boolean
}>(), {
  page: 0,
  totalPages: 0,
  totalRows: 0,
  hasNext: false,
  hasPrevious: false,
})

const emit = defineEmits<{
  'page-change': [page: number]
}>()

const { selectedIana } = useDisplayTimezone()

function fmtTime(iso: string): string {
  const tz = selectedIana.value
  const d = new Date(iso)
  if (props.selectedRange === '1h') {
    return d.toLocaleTimeString('en-GB', { timeZone: tz, hour: '2-digit', minute: '2-digit', second: '2-digit' })
  }
  if (props.selectedRange === '7d') {
    return d.toLocaleDateString('en-GB', { timeZone: tz, weekday: 'short', day: 'numeric', month: 'short' }) +
           ' ' + d.toLocaleTimeString('en-GB', { timeZone: tz, hour: '2-digit', minute: '2-digit' })
  }
  return d.toLocaleDateString('en-GB', { timeZone: tz, day: 'numeric', month: 'short', year: '2-digit' }) +
         ' ' + d.toLocaleTimeString('en-GB', { timeZone: tz, hour: '2-digit', minute: '2-digit' })
}

function fmtHeap(s: Snapshot): string {
  if (s.memoryUsedBytes == null) return '—'
  const mb = s.memoryUsedBytes / (1024 ** 2)
  if (mb >= 1024) return (mb / 1024).toFixed(2) + ' GB'
  return Math.round(mb) + ' MB'
}

function fmtGc(s: Snapshot): string {
  if (s.gcOverhead == null) return '—'
  const p = s.gcOverhead * 100
  return p < 0.01 ? '<0.01%' : p.toFixed(2) + '%'
}

function fmtCpu(s: Snapshot): string {
  if (s.cpuUsage == null) return '—'
  return (s.cpuUsage * 100).toFixed(1) + '%'
}

function fmtAvgMs(s: Snapshot): string {
  if (s.httpAvgMs == null) return '—'
  return s.httpAvgMs.toFixed(1) + ' ms'
}

function cpuColor(s: Snapshot): string {
  if (s.cpuUsage == null) return 'var(--text-muted)'
  const pct = s.cpuUsage * 100
  if (pct > 85) return 'hsl(0,85%,60%)'
  if (pct > 65) return 'hsl(38,95%,55%)'
  return 'hsl(142,70%,45%)'
}

function gcColor(s: Snapshot): string {
  if (s.gcOverhead == null) return 'var(--text-muted)'
  const pct = s.gcOverhead * 100
  if (pct > 5) return 'hsl(0,85%,60%)'
  if (pct > 2) return 'hsl(38,95%,55%)'
  return 'hsl(142,70%,45%)'
}

function healthColor(status: string): string {
  return status === 'UP' ? 'hsl(142,70%,45%)' : 'hsl(0,85%,60%)'
}

function healthBg(status: string): string {
  return status === 'UP' ? 'hsla(142,70%,45%,0.1)' : 'hsla(0,85%,60%,0.1)'
}
</script>

<template>
  <div v-if="snapshots.length === 0" class="tbl-empty">No snapshots in this range.</div>

  <div v-else class="tbl-card">
    <div class="tbl-header">
      <div class="tbl-title"><strong>Snapshot History</strong></div>
      <div class="tbl-sub">View History of snapshots stored in DB</div>
    </div>

    <div class="tbl-scroll">
      <table class="data-tbl">
        <thead>
          <tr>
            <th>Time</th>
            <th>Health</th>
            <th>CPU Usage</th>
            <th>JVM Heap Memory</th>
            <th>GC Overhead</th>
            <th class="sep">HTTP Avg Response</th>
            <th>2xx</th>
            <th>3xx</th>
            <th>4xx</th>
            <th>5xx</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="s in snapshots" :key="s.id">
            <td class="td-time" data-label="Time">{{ fmtTime(s.polledAt) }}</td>
            <td data-label="Health">
              <span class="td-badge"
                :style="{ color: healthColor(s.healthStatus), background: healthBg(s.healthStatus) }">
                {{ s.healthStatus }}
              </span>
            </td>
            <td data-label="CPU Usage">
              <span class="td-val" :style="{ color: cpuColor(s) }">{{ fmtCpu(s) }}</span>
            </td>
            <td class="td-val" data-label="JVM Heap">{{ fmtHeap(s) }}</td>
            <td data-label="GC Overhead">
              <span class="td-val" :style="{ color: gcColor(s) }">{{ fmtGc(s) }}</span>
            </td>
            <td class="td-val sep" data-label="Avg Response">{{ fmtAvgMs(s) }}</td>
            <td class="td-val c-2xx" data-label="2xx">{{ (s.http2xxCount ?? 0).toLocaleString() }}</td>
            <td class="td-val c-3xx" data-label="3xx">{{ (s.http3xxCount ?? 0).toLocaleString() }}</td>
            <td data-label="4xx">
              <span class="td-val" :style="{ color: (s.http4xxCount ?? 0) > 0 ? 'hsl(48,90%,40%)' : 'var(--text-secondary)' }">
                {{ (s.http4xxCount ?? 0).toLocaleString() }}
              </span>
            </td>
            <td data-label="5xx">
              <span class="td-val" :style="{ color: (s.http5xxCount ?? 0) > 0 ? 'hsl(0,85%,60%)' : 'var(--text-secondary)' }">
                {{ (s.http5xxCount ?? 0).toLocaleString() }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination footer -->
    <div class="tbl-footer">
      <span class="pg-info">
        Page {{ (page ?? 0) + 1 }} of {{ totalPages || 1 }}
        <span class="pg-total">· {{ (totalRows ?? 0).toLocaleString() }} total</span>
      </span>
      <div class="pg-buttons">
        <button class="pg-btn" :disabled="!hasPrevious" @click="emit('page-change', page - 1)">← Prev</button>
        <button class="pg-btn" :disabled="!hasNext"     @click="emit('page-change', page + 1)">Next →</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.tbl-empty {
  padding: 48px 0;
  text-align: center;
  font-size: 13px;
  color: var(--text-muted);
}

.tbl-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 10px;
  overflow: hidden;
}

.tbl-header {
  padding: 16px 20px 14px;
  border-bottom: 1px solid var(--border);
}
.tbl-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 2px;
}
.tbl-sub {
  font-size: 11px;
  color: var(--text-muted);
}

.tbl-scroll {
  overflow-x: auto;
}

/* Table */
.data-tbl {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.data-tbl thead tr {
  border-bottom: 1px solid var(--border);
}

.data-tbl th {
  padding: 11px 16px;
  text-align: left;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
}

.data-tbl tbody tr {
  border-bottom: 1px solid var(--border);
  transition: background 0.1s;
}
.data-tbl tbody tr:last-child { border-bottom: none; }
.data-tbl tbody tr:hover { background: var(--nav-hover); }

.data-tbl td {
  padding: 10px 16px;
  white-space: nowrap;
}

/* HTTP section separator */
.sep {
  border-left: 1px solid var(--border);
}

/* Cell types */
.td-time {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.td-val {
  font-size: 13px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--text-secondary);
}

.td-badge {
  display: inline-block;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.04em;
  padding: 2px 9px;
  border-radius: 10px;
}

/* HTTP status colors */
.c-2xx { color: hsl(142, 70%, 45%); }
.c-3xx { color: hsl(28,  90%, 50%); }

/* Pagination footer */
.tbl-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-top: 1px solid var(--border);
  gap: 12px;
}

.pg-info {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-primary);
}

.pg-total {
  font-weight: 400;
  color: var(--text-muted);
}

.pg-buttons {
  display: flex;
  gap: 6px;
}

.pg-btn {
  font-size: 12px;
  font-weight: 600;
  padding: 5px 12px;
  border-radius: 6px;
  border: 1px solid var(--border);
  background: transparent;
  color: var(--text-primary);
  cursor: pointer;
  transition: all 0.13s;
}
.pg-btn:hover:not(:disabled) { border-color: var(--accent); color: var(--accent); }
.pg-btn:disabled { opacity: 0.35; cursor: not-allowed; }

/* Mobile: each row becomes a labeled card */
@media (max-width: 700px) {
  .tbl-scroll { overflow-x: unset; }

  .data-tbl thead { display: none; }

  .data-tbl,
  .data-tbl tbody,
  .data-tbl tr,
  .data-tbl td {
    display: block;
    width: 100%;
  }

  .data-tbl tbody tr {
    border: 1px solid var(--border);
    border-radius: 10px;
    margin: 10px 12px;
    width: calc(100% - 24px);
    padding: 4px 0;
  }

  .data-tbl tbody tr:last-child { border-bottom: 1px solid var(--border); }

  .data-tbl td {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 9px 14px;
    border-bottom: 1px solid var(--border);
    white-space: normal;
  }

  .data-tbl td:last-child { border-bottom: none; }

  /* Label from data-label attribute */
  .data-tbl td::before {
    content: attr(data-label);
    font-size: 11px;
    font-weight: 700;
    color: var(--text-muted);
    text-transform: uppercase;
    letter-spacing: 0.06em;
    flex-shrink: 0;
    margin-right: 12px;
  }

  /* Remove HTTP separator on mobile */
  .sep { border-left: none; }

  .tbl-footer { flex-direction: column; align-items: flex-start; gap: 8px; padding: 12px; }
}
</style>
