const BASE_URL = 'http://localhost:8080'
const TOKEN_KEY = 'access_token'

async function rawFetch(path: string, method = 'GET'): Promise<string> {
  const token = localStorage.getItem(TOKEN_KEY)
  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers: { Authorization: `Bearer ${token ?? ''}` },
  })
  if (!res.ok) throw new Error(await res.text())
  return res.text()
}

export function generateUserSecret(): Promise<string> {
  return rawFetch('/api/v0/user/secret', 'PUT')
}

export function deleteUser(): Promise<string> {
  return rawFetch('/api/v0/user', 'DELETE')
}

export function getUserSnapshotSize(): Promise<string> {
  return rawFetch('/api/v0/user/snapshots/size')
}
