import { ref, computed } from 'vue'

const TOKEN_KEY = 'access_token'

export interface JwtPayload {
  iss: string
  sub: string
  exp: number
  userId: string
  iat: number
  createdAt: string
  role: string
  scope: string[]
}

const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))

function parsePayload(t: string): JwtPayload | null {
  try {
    const part = t.split('.')[1]
    if (!part) return null
    return JSON.parse(atob(part)) as JwtPayload
  } catch {
    return null
  }
}

const tokenPayload = computed<JwtPayload | null>(() =>
  token.value ? parsePayload(token.value) : null,
)

const isAuthenticated = computed(() => {
  const p = tokenPayload.value
  return !!p && p.exp * 1000 > Date.now()
})

function setToken(accessToken: string) {
  token.value = accessToken
  localStorage.setItem(TOKEN_KEY, accessToken)
}

function clearToken() {
  token.value = null
  localStorage.clear()
}

function isTokenExpired(): boolean {
  const p = tokenPayload.value
  return !p || p.exp * 1000 <= Date.now()
}

export function useAuth() {
  return { token, tokenPayload, isAuthenticated, setToken, clearToken, isTokenExpired }
}
