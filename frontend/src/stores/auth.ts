import { defineStore } from 'pinia'
import { currentUser, login as loginApi, type CurrentUser, type MenuNode } from '../api'

type LoginSession = Awaited<ReturnType<typeof loginApi>>

const TOKEN_KEY = 'ERP_TOKEN'
const PRIMARY_STUDENT_SESSION = 'ERP_PRIMARY_STUDENT_SESSION'

type StoredSession = {
  token: string
  user: CurrentUser
  menus: MenuNode[]
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: null as CurrentUser | null,
    menus: [] as MenuNode[]
  }),
  actions: {
    applySession(data: LoginSession) {
      this.token = data.token
      this.user = data.user
      this.menus = data.menus
      localStorage.setItem(TOKEN_KEY, data.token)
    },
    async login(username: string, password: string) {
      const data = await loginApi({ username, password })
      localStorage.removeItem(PRIMARY_STUDENT_SESSION)
      this.applySession(data)
      return data
    },
    async loadCurrentUser() {
      if (!this.token) return
      const data = await currentUser()
      this.user = data.user
      this.menus = data.menus
    },
    savePrimaryStudentSession() {
      if (this.user?.role !== 'STUDENT' || !this.token) return
      const session: StoredSession = {
        token: this.token,
        user: this.user,
        menus: this.menus
      }
      localStorage.setItem(PRIMARY_STUDENT_SESSION, JSON.stringify(session))
    },
    restorePrimaryStudentSession() {
      const raw = localStorage.getItem(PRIMARY_STUDENT_SESSION)
      if (!raw) return false
      try {
        const session = JSON.parse(raw) as StoredSession
        if (!session.token || session.user?.role !== 'STUDENT') return false
        this.token = session.token
        this.user = session.user
        this.menus = session.menus || []
        localStorage.setItem(TOKEN_KEY, session.token)
        localStorage.removeItem(PRIMARY_STUDENT_SESSION)
        return true
      } catch {
        localStorage.removeItem(PRIMARY_STUDENT_SESSION)
        return false
      }
    },
    logoutCurrentIdentity() {
      if (this.restorePrimaryStudentSession()) {
        return 'student'
      }
      this.logout()
      return 'login'
    },
    logout() {
      this.token = ''
      this.user = null
      this.menus = []
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(PRIMARY_STUDENT_SESSION)
    }
  }
})
