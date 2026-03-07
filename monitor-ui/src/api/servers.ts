import { apiFetch } from './client'

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
