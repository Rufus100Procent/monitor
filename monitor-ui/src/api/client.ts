const BASE_URL = 'http://localhost:8080'
const TOKEN_KEY = 'access_token'

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

export async function apiFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const token = getValidToken()

  if (!token) {
    window.location.href = '/'
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
      window.location.href = '/'
      throw new Error('Unauthorized')
    }
    const err: ApiErrorResponse = await res.json()
    throw new Error(err.message)
  }

  return res.json() as Promise<T>
}
