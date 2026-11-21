import { defineStore } from 'pinia'
import { fetchMe, login as loginApi, logout as logoutApi, type MeInfo } from '@/api/auth'
import { clearToken, getToken, setToken } from '@/api/http'

/** 博客自有账号登录态：账号、令牌、按钮权限码全部来自独立博客服务。 */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken(),
    user: null as MeInfo | null,
    ready: false
  }),

  getters: {
    isLogin: (s) => !!s.token && !!s.user,
    isSuperAdmin: (s) => !!s.user?.superAdmin,
    permissions: (s) => s.user?.permissions || [],
    nickname: (s) => s.user?.nickname || s.user?.username || '未登录'
  },

  actions: {
    /** 前端隐藏按钮只是体验，真正的拦截在后端 @Permit 切面。 */
    can(code: string): boolean {
      if (!this.user) return false
      return this.user.superAdmin || (this.user.permissions || []).includes(code)
    },

    async bootstrap(): Promise<boolean> {
      if (this.ready && this.user) return true
      const token = getToken()
      if (!token) {
        this.ready = true
        return false
      }
      try {
        this.user = await fetchMe()
        this.token = token
        this.ready = true
        return true
      } catch {
        clearToken()
        this.token = ''
        this.user = null
        this.ready = true
        return false
      }
    },

    /** 账号密码登录，成功落令牌并加载身份 */
    async login(username: string, password: string): Promise<boolean> {
      const result = await loginApi(username, password)
      setToken(result.accessToken)
      this.token = result.accessToken
      this.user = {
        userId: result.userId,
        username: result.username,
        nickname: result.nickname,
        avatar: result.avatar,
        superAdmin: result.superAdmin,
        permissions: result.permissions
      }
      this.ready = true
      return true
    },

    async logout() {
      try { await logoutApi() } catch { /* 本地令牌无论如何都要清 */ }
      clearToken()
      this.token = ''
      this.user = null
      this.ready = false
    }
  }
})
