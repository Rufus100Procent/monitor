const BASE_URL = 'http://localhost:8080'

interface ApiErrorResponse {
  message: string
  timestamp: string
  status: number
}

export async function apiFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
    ...options,
  })

  if (!res.ok) {
    const err: ApiErrorResponse = await res.json()
    throw new Error(err.message)
  }

  return res.json() as Promise<T>
}
