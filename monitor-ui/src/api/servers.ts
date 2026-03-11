import { apiFetch } from './client'

const BASE_URL = 'http://localhost:8080'
const TOKEN_KEY = 'access_token'

export interface Server {
  id: string
  appName: string
  appVersion?: string
  ServerActuatorUrl: string
  cpuCoreCount: number
  memoryMaxBytes: number
  pause: boolean
  pollIntervalSeconds: number
  status: 'UP' | 'DOWN'
  lastPolledAt: string
  lastSeenUp: string
  registeredAt: string
}

export interface RegisterServerBody {
  actuatorPath: string
  baseUrl: string
  appName: string
  appVersion: string
  pollIntervalSeconds: number
}

export function getServers(): Promise<Server[]> {
  return apiFetch<Server[]>('/api/v0/server')
}

export interface UpdateServerBody {
  id: string
  appName: string
  appVersion: string
  pollIntervalSeconds: number
  pause: boolean
}

export function registerServer(body: RegisterServerBody): Promise<{ secret: string }> {
  return apiFetch<{ secret: string }>('/api/v0/server', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function updateServer(body: UpdateServerBody): Promise<Server> {
  return apiFetch<Server>('/api/v0/server', {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}

export function deleteServer(id: string): Promise<void> {
  return apiFetch<void>(`/api/v0/server/${id}`, { method: 'DELETE' })
}

export interface Snapshot {
  id: string
  serverId: string
  polledAt: string
  pollSuccess: boolean
  healthStatus: 'UP' | 'DOWN'
  cpuUsage: number
  systemLoad: number
  jvmThreadsLive: number
  memoryUsedBytes: number
  diskFreeBytes: number
  diskTotalBytes: number
  uptimeSeconds: number
  gcOverhead: number
  httpRequestCount: number
  http2xxCount: number
  http3xxCount: number
  http4xxCount: number
  http5xxCount: number
  httpAvgMs: number
}

export interface SnapshotPage {
  data: Snapshot[]
  totalRows: number
  page: number
  size: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
}

export interface SnapshotQueryParams {
  from?: string
  to?: string
  page?: number
  size?: number
}

export function getSnapshots(serverId: string, params?: SnapshotQueryParams): Promise<SnapshotPage> {
  const q = new URLSearchParams()
  if (params?.from)          q.set('from', params.from)
  if (params?.to)            q.set('to', params.to)
  if (params?.page != null)  q.set('page', String(params.page))
  if (params?.size != null)  q.set('size', String(params.size))
  const qs = q.toString()
  return apiFetch<SnapshotPage>(`/api/v0/snapshot/${serverId}${qs ? '?' + qs : ''}`)
}

export function getLatestSnapshot(serverId: string): Promise<Snapshot> {
  return apiFetch<Snapshot>(`/api/v0/snapshot/${serverId}/latest`)
}

export async function getSnapshotSize(serverId: string): Promise<string> {
  const token = localStorage.getItem(TOKEN_KEY)
  const res = await fetch(`${BASE_URL}/api/v0/snapshot/${serverId}/size`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!res.ok) throw new Error('Failed to fetch snapshot size')
  return res.text()
}

export async function generateSecret(serverId: string): Promise<string> {
  const token = localStorage.getItem(TOKEN_KEY)
  const res = await fetch(`${BASE_URL}/api/v0/server/${serverId}/secret`, {
    method: 'PUT',
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!res.ok) throw new Error('Failed to generate secret')
  return res.text()
}

export async function exportSnapshotsCsv(serverId: string): Promise<void> {
  const token = localStorage.getItem(TOKEN_KEY)
  const res = await fetch(`${BASE_URL}/api/v0/snapshot/${serverId}/export`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!res.ok) throw new Error('Export failed')
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `snapshots-${serverId}.csv`
  a.click()
  URL.revokeObjectURL(url)
}
