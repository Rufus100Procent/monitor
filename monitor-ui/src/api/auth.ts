const BASE_URL = 'http://localhost:8080'

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
