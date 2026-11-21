import http from './http'

/** 博客自有账号登录结果，字段与后端 LoginVO 一一对应。 */
export interface LoginResult {
  accessToken: string
  userId: number
  username: string
  nickname: string
  avatar: string
  superAdmin: boolean
  permissions: string[]
}

export type MeInfo = Omit<LoginResult, 'accessToken'>

export function login(username: string, password: string): Promise<LoginResult> {
  return http.post('/auth/login', { username, password })
}

export function fetchMe(): Promise<MeInfo> {
  return http.get('/auth/me')
}

export function logout(): Promise<void> {
  return http.post('/auth/logout')
}

/** 当前登录人修改密码：原密码校验通过后更新为新密码。 */
export function changePassword(oldPassword: string, newPassword: string): Promise<void> {
  return http.post('/auth/change-password', { oldPassword, newPassword })
}
