import { defineStore } from 'pinia'
import { currentUser, login, type CurrentUser, type MenuNode } from '../api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('ERP_TOKEN') || '',
    user: null as CurrentUser | null,
    menus: [] as MenuNode[]
  }),
  actions: {
    async login(username: string, password: string) {
      const data = await login({ username, password })
      this.token = data.token
      this.user = data.user
      this.menus = data.menus
      localStorage.setItem('ERP_TOKEN', data.token)
    },
    async loadCurrentUser() {
      if (!this.token) return
      const data = await currentUser()
      this.user = data.user
      this.menus = data.menus
    },
    logout() {
      this.token = ''
      this.user = null
      this.menus = []
      localStorage.removeItem('ERP_TOKEN')
    }
  }
})

