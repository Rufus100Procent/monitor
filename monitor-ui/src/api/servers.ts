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

export function registerServer(body: RegisterServerBody): Promise<Server> {
  return apiFetch<Server>('/api/v0/server', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}
