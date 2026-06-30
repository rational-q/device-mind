import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const currentUser = ref('admin')

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  return { sidebarCollapsed, currentUser, toggleSidebar }
})
