import { ref } from 'vue'
import { getServers } from '../api/servers'
import type { Server } from '../api/servers'

// Module-level singleton — shared across all components
const servers = ref<Server[]>([])
const loading = ref(false)
const loaded = ref(false)
const error = ref('')

export function useServers() {
  async function fetch() {
    if (loading.value) return
    loading.value = true
    error.value = ''
    try {
      servers.value = await getServers()
      loaded.value = true
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to load servers.'
    } finally {
      loading.value = false
    }
  }

  async function ensureLoaded() {
    if (!loaded.value) await fetch()
  }

  function addServer(server: Server) {
    servers.value = [server, ...servers.value]
  }

  function removeServer(id: string) {
    servers.value = servers.value.filter(s => s.id !== id)
  }

  function updateServerInList(updated: Server) {
    const idx = servers.value.findIndex(s => s.id === updated.id)
    if (idx !== -1) servers.value[idx] = updated
  }

  return { servers, loading, loaded, error, fetch, ensureLoaded, addServer, removeServer, updateServerInList }
}
