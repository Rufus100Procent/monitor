<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useTheme } from '../composables/useTheme'
import { useSidebar } from '../composables/useSidebar'
import { Sun, Moon, Menu } from 'lucide-vue-next'

const { dark, toggle } = useTheme()
const { toggle: toggleSidebar } = useSidebar()

const ZONES = [
  { city: 'Stockholm', flag: '🇸🇪', iana: 'Europe/Stockholm' },
  { city: 'London',    flag: '🇬🇧', iana: 'Europe/London'    },
  { city: 'New York',  flag: '🇺🇸', iana: 'America/New_York' },
  { city: 'Tokyo',     flag: '🇯🇵', iana: 'Asia/Tokyo'       },
  { city: 'Dubai',     flag: '🇦🇪', iana: 'Asia/Dubai'       },
  { city: 'Sydney',    flag: '🇦🇺', iana: 'Australia/Sydney' },
  { city: 'São Paulo', flag: '🇧🇷', iana: 'America/Sao_Paulo'},
]

const selectedZone = ref<typeof ZONES[number]>(ZONES[0]!)
const now = ref(new Date())
const showDropdown = ref(false)
const dropdownRef = ref<HTMLElement | null>(null)

let timer: ReturnType<typeof setInterval>

onMounted(() => {
  timer = setInterval(() => { now.value = new Date() }, 1000)
  document.addEventListener('click', onOutsideClick)
})

onBeforeUnmount(() => {
  clearInterval(timer)
  document.removeEventListener('click', onOutsideClick)
})

function onOutsideClick(e: MouseEvent) {
  if (dropdownRef.value && !dropdownRef.value.contains(e.target as Node)) {
    showDropdown.value = false
  }
}

const formattedDateTime = computed(() => {
  const date = now.value.toLocaleDateString('en-GB', {
    timeZone: selectedZone.value.iana,
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })
  const time = now.value.toLocaleTimeString('en-GB', {
    timeZone: selectedZone.value.iana,
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
  return { date, time }
})

function selectZone(zone: typeof ZONES[number]) {
  selectedZone.value = zone
  showDropdown.value = false
}
</script>

<template>
  <header class="topbar">
    <div class="topbar-left">
      <button class="menu-btn" @click="toggleSidebar" aria-label="Toggle navigation">
        <Menu :size="20" />
      </button>
    </div>

    <div class="topbar-center" ref="dropdownRef">
      <button class="zone-trigger" @click.stop="showDropdown = !showDropdown">
        <span class="zone-flag">{{ selectedZone.flag }}</span>
        <span class="zone-city">{{ selectedZone.city }}</span>
        <span class="zone-sep">·</span>
        <span class="zone-date">{{ formattedDateTime.date }}</span>
        <span class="zone-sep">·</span>
        <span class="zone-time">{{ formattedDateTime.time }}</span>
        <span class="i-arrow" :class="{ open: showDropdown }">▼</span>
      </button>

      <div class="gr-wrap" :class="{ open: showDropdown }">
        <div class="gr-inner">
          <div class="zone-dropdown">
            <button
              v-for="z in ZONES"
              :key="z.iana"
              class="zone-option"
              :class="{ active: z.iana === selectedZone.iana }"
              @click="selectZone(z)"
            >
              <span>{{ z.flag }}</span>
              <span>{{ z.city }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="topbar-right">
      <button class="theme-btn" @click="toggle" :title="dark ? 'Light mode' : 'Dark mode'">
        <Sun v-if="dark" :size="18" />
        <Moon v-else :size="18" />
      </button>
    </div>
  </header>
</template>

<style scoped>
.topbar {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  height: 52px;
  padding: 0 16px;
  background: var(--topbar-bg);
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.topbar-left {
  display: flex;
  align-items: center;
}

.menu-btn {
  display: none;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  color: var(--text-secondary);
  transition: background 0.15s, color 0.15s;
}

.menu-btn:hover {
  background: var(--nav-hover);
  color: var(--text-primary);
}

@media (max-width: 768px) {
  .menu-btn {
    display: flex;
  }
}

.topbar-center {
  position: relative;
  display: flex;
  justify-content: center;
}

.topbar-right {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.zone-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  transition: background 0.15s, color 0.15s;
}

.zone-trigger:hover {
  background: var(--nav-hover);
  color: var(--text-primary);
}

.zone-flag {
  font-size: 15px;
  line-height: 1;
}

.zone-city {
  color: var(--text-primary);
  font-weight: 600;
}

.zone-sep {
  color: var(--text-muted);
}

.zone-time {
  font-variant-numeric: tabular-nums;
  font-weight: 600;
  color: var(--text-primary);
}

.i-arrow {
  font-size: 0.58rem;
  color: var(--text-muted);
  margin-left: 2px;
  transition: transform 0.28s ease, color 0.15s;
  line-height: 1;
}
.i-arrow.open {
  transform: rotate(180deg);
  color: var(--text-secondary);
}

/* grid-row expand/collapse */
.gr-wrap {
  position: absolute;
  top: calc(100% + 4px);
  left: 50%;
  transform: translateX(-50%);
  display: grid;
  grid-template-rows: 0fr;
  transition: grid-template-rows 0.3s ease;
  z-index: 100;
  filter: drop-shadow(0 8px 16px hsl(220, 20%, 10%, 0.12));
}
.gr-wrap.open {
  grid-template-rows: 1fr;
}
.gr-inner {
  overflow: hidden;
}

.zone-dropdown {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 4px;
  min-width: 160px;
}

.zone-option {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px 10px;
  border-radius: 7px;
  font-size: 13px;
  color: var(--text-secondary);
  transition: background 0.1s, color 0.1s;
}

.zone-option:hover {
  background: var(--nav-hover);
  color: var(--text-primary);
}

.zone-option.active {
  background: hsl(270, 80%, 95%);
  color: var(--nav-active-text);
  font-weight: 600;
}

:root.dark .zone-option.active {
  background: hsl(270, 50%, 20%);
}

/* theme button */
.theme-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  color: var(--text-secondary);
  transition: background 0.15s, color 0.15s;
}

.theme-btn:hover {
  background: var(--nav-hover);
  color: var(--text-primary);
}

@media (max-width: 768px) {
  .zone-city,
  .zone-date,
  .zone-sep {
    display: none;
  }

  .gr-wrap {
    position: fixed;
    top: 56px;
    left: 50%;
    transform: translateX(-50%);
  }

  .zone-dropdown {
    min-width: 200px;
  }
}
</style>
