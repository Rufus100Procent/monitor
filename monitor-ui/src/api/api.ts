const BASE_URL = 'http://localhost:8080'
const TOKEN_KEY = 'access_token'

// HTTP Client

interface ApiErrorResponse {
  message: string
  timestamp: string
  status: number
}

function getValidToken(): string | null {
  const token = localStorage.getItem(TOKEN_KEY)
  if (!token) return null
  try {
    const payload = JSON.parse(atob(token.split('.')[1] ?? ''))
    if (payload.exp * 1000 <= Date.now()) {
      localStorage.removeItem(TOKEN_KEY)
      return null
    }
    return token
  } catch {
    localStorage.removeItem(TOKEN_KEY)
    return null
  }
}

async function apiFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const token = getValidToken()

  if (!token) {
    globalThis.location.href = '/'
    throw new Error('Session expired')
  }

  const { headers: extraHeaders, ...restOptions } = options ?? {}

  const res = await fetch(`${BASE_URL}${path}`, {
    ...restOptions,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
      ...extraHeaders,
    },
  })

  if (!res.ok) {
    if (res.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      globalThis.location.href = '/'
      throw new Error('Unauthorized')
    }
    const err: ApiErrorResponse = await res.json()
    throw new Error(err.message)
  }

  return res.json() as Promise<T>
}

async function apiFetchText(path: string, method = 'GET'): Promise<string> {
  const token = localStorage.getItem(TOKEN_KEY)
  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers: { Authorization: `Bearer ${token ?? ''}` },
  })
  if (!res.ok) throw new Error(await res.text())
  return res.text()
}

//  Auth

export async function loginUser(username: string, password: string): Promise<{ accessToken: string }> {
  const res = await fetch(`${BASE_URL}/api/v0/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: 'Login failed' }))
    throw new Error(err.message ?? 'Login failed')
  }
  return res.json()
}

export async function registerUser(username: string, password: string): Promise<string> {
  const res = await fetch(`${BASE_URL}/api/v0/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: 'Registration failed' }))
    throw new Error(err.message ?? 'Registration failed')
  }
  return res.text()
}

//  Servers

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

export interface UpdateServerBody {
  id: string
  appName: string
  appVersion: string
  pollIntervalSeconds: number
  pause: boolean
}

export function getServers(): Promise<Server[]> {
  return apiFetch<Server[]>('/api/v0/server')
}

export function registerServer(body: RegisterServerBody): Promise<Server> {
  return apiFetch<Server>('/api/v0/server', {
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

//  Snapshots

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
  if (params?.from)         q.set('from', params.from)
  if (params?.to)           q.set('to', params.to)
  if (params?.page != null) q.set('page', String(params.page))
  if (params?.size != null) q.set('size', String(params.size))
  const qs = q.toString()
  return apiFetch<SnapshotPage>(`/api/v0/snapshot/${serverId}${qs ? '?' + qs : ''}`)
}

export function getLatestSnapshot(serverId: string): Promise<Snapshot> {
  return apiFetch<Snapshot>(`/api/v0/snapshot/${serverId}/latest`)
}

export function getSnapshotSize(serverId: string): Promise<string> {
  return apiFetchText(`/api/v0/snapshot/${serverId}/size`)
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

//  User

export function generateUserSecret(): Promise<string> {
  return apiFetchText('/api/v0/user/secret', 'PUT')
}

export function deleteUser(): Promise<string> {
  return apiFetchText('/api/v0/user', 'DELETE')
}

export function getUserSnapshotSize(): Promise<string> {
  return apiFetchText('/api/v0/user/snapshots/size')
}
