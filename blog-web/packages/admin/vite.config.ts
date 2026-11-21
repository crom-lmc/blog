import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'VITE_')
  const port = Number(env.VITE_ADMIN_PORT || 5174)
  const apiTarget = env.VITE_API_TARGET || 'http://127.0.0.1:9003'
  const webOrigin = env.VITE_WEB_ORIGIN || 'http://localhost:5175'

  return {
    // 与博客前台共用同一域名，管理端挂在 /admin/ 子路径（hash 路由，API 走绝对路径 /api/blog）
    base: '/admin/',
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
        '@blog/shared': fileURLToPath(new URL('./src/shared', import.meta.url))
      }
    },
    define: {
      __WEB_ORIGIN__: JSON.stringify(webOrigin)
    },
    server: {
      port,
      host: true,
      proxy: {
        '/api': { target: apiTarget, changeOrigin: true },
        '/uploads': { target: apiTarget, changeOrigin: true }
      }
    },
    build: { outDir: 'dist', chunkSizeWarningLimit: 1800 }
  }
})
