import { ref } from 'vue'

// Module-level singleton — shared across all components
const selectedIana = ref('Europe/Stockholm')

export function useDisplayTimezone() {
  function setDisplayTimezone(iana: string) {
    selectedIana.value = iana
  }

  return { selectedIana, setDisplayTimezone }
}
