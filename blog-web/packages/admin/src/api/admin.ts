import http from './http'

export interface SysUser {
  id: number
  username: string
  nickname: string
  email: string
  status: number
  roleCodes: string[]
  lastLoginAt?: string
}

export interface UserSavePayload {
  id?: number
  username: string
  nickname?: string
  email?: string
  password?: string
  status?: number
  roleIds: number[]
}

export interface SysRole {
  id: number
  code: string
  name: string
  description?: string
  sortNo: number
  permissions: string[]
}

export interface SysPermission {
  id: number
  code: string
  name: string
  type: 'MENU' | 'BUTTON'
  sortNo: number
}

// http 拦截器已经把 R 壳拆掉，这里显式标注业务返回类型（不要写 axios 泛型，
// 否则拿到的是 AxiosResponse 而不是 data）。
export const listUsers = (): Promise<SysUser[]> => http.get('/admin/users')
export const saveUser = (payload: UserSavePayload): Promise<number> => http.post('/admin/users', payload)
export const deleteUser = (id: number): Promise<void> => http.delete(`/admin/users/${id}`)
export const resetUserPassword = (id: number, password: string): Promise<void> =>
  http.put(`/admin/users/${id}/password`, { password })

export const listRoles = (): Promise<SysRole[]> => http.get('/admin/roles')
export const saveRole = (payload: Partial<SysRole> & { permissions: string[] }): Promise<number> =>
  http.post('/admin/roles', payload)
export const deleteRole = (id: number): Promise<void> => http.delete(`/admin/roles/${id}`)
export const listPermissions = (): Promise<SysPermission[]> => http.get('/admin/roles/permissions')
