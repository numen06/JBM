import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import path from 'node:path'
import type { IncomingMessage } from 'node:http'

/** jaja7 本地：所有 API 经 Gateway 6060（需同时启动 Gateway / Auth / Center） */
const gateway = 'http://127.0.0.1:6060'

const gatewayPrefixes = [
  '/auth',
  '/center',
  '/jbm-cluster-platform-',
]

function proxyMap(target: string, prefixes: string[]) {
  return Object.fromEntries(
    prefixes.map((p) => [
      p,
      {
        target,
        changeOrigin: true,
        bypass(req: IncomingMessage) {
          const accept = req.headers.accept || ''
          if (req.method === 'GET' && accept.includes('text/html')) {
            return '/index.html'
          }
        },
      },
    ]),
  )
}

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    // 同时监听 127.0.0.1 与 ::1，避免仅 IPv6 localhost 导致 127.0.0.1 无法访问
    host: true,
    proxy: proxyMap(gateway, gatewayPrefixes),
  },
})
