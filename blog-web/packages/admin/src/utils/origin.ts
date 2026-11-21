/**
 * 博客前台站点地址。
 *
 * 生产环境管理端与前台同域部署（前台在 `/`、管理端在 `/admin/`），
 * 直接用当前 origin 拼接即可，避免写死 localhost:5173/5175 导致
 * 线上「查看前台」「主题编辑器预览」跳到本地或 SSO 登录页。
 *
 * 本地开发时两端不同端口，可用环境变量 VITE_WEB_ORIGIN 覆盖；
 * 未配置时按开发默认端口回退。
 */
export function getWebOrigin(): string {
  const configured = (import.meta.env.VITE_WEB_ORIGIN as string | undefined)?.trim()
  if (configured) return configured.replace(/\/+$/, '')
  if (import.meta.env.PROD && typeof window !== 'undefined') return window.location.origin
  return 'http://localhost:5175'
}
