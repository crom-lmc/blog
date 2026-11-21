import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
// 本工程已把 shared 源码内联到 src/shared，mock 插件同样从本地引入，不依赖任何外部工作区路径。
// 用相对路径引入：Vite 加载配置时会一起打包，避免被当作外部依赖由 Node 直接加载 TS。
import { createMockApiPlugin } from './src/shared/mock/plugin'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'VITE_')
  // Mock 为可选能力：默认关闭（VITE_USE_MOCK=false），走真实后端；
  // 仅当显式设置 VITE_USE_MOCK=true 时挂载内置 Mock 服务。
  const useMock = env.VITE_USE_MOCK === 'true'
  const webHost = env.VITE_WEB_HOST || 'localhost'
  const webPort = Number(env.VITE_WEB_PORT || 5175)
  // 默认指向独立博客服务端口 9003
  const apiTarget = env.VITE_API_TARGET || 'http://127.0.0.1:9003'
  const webOrigin = env.VITE_WEB_ORIGIN || `http://${webHost}:${webPort}`

  return {
    plugins: [vue(), ...(useMock ? [createMockApiPlugin({ webOrigin })] : [])],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
        '@blog/shared': fileURLToPath(new URL('./src/shared', import.meta.url))
      }
    },
    server: {
      port: webPort,
      host: true,
      // Mock 关闭时把 /api、/uploads 代理到独立博客服务
      proxy: useMock
        ? undefined
        : {
            '/api': { target: apiTarget, changeOrigin: true },
            '/uploads': { target: apiTarget, changeOrigin: true }
          }
    },
    build: { outDir: 'dist', sourcemap: false }
  }
})
