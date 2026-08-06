import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import path from 'node:path'
import type { IncomingMessage } from 'node:http'

/** dev 本地：默认经本机 Gateway 6060；也可用 VITE_PROXY_TARGET 指向已发布后台。 */
function resolveGatewayTarget(mode: string) {
  const env = loadEnv(mode, process.cwd(), '')
  return env.VITE_PROXY_TARGET || 'http://127.0.0.1:6060'
}

const gatewayPrefixes = [
  '/v3/api',
  '/auth',
  '/center',
  '/push',
  '/jbm-cluster-platform-',
]

function proxyMap(target: string, prefixes: string[]) {
  return Object.fromEntries(
    prefixes.map((p) => [
      p,
      {
        target,
        changeOrigin: true,
        ws: true,
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

export default defineConfig(({ mode }) => ({
  plugins: [vue(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      '@jbm7/sdk': path.resolve(__dirname, './packages/sdk/src/index.ts'),
      '@jbm7/vue-core': path.resolve(__dirname, './packages/vue-core/src/index.ts'),
      '@jbm7/admin': path.resolve(__dirname, './packages/admin/src/index.ts'),
    },
  },
  server: {
    port: 5173,
    // 同时监听 127.0.0.1 与 ::1，避免仅 IPv6 localhost 导致 127.0.0.1 无法访问
    host: true,
    proxy: proxyMap(resolveGatewayTarget(mode), gatewayPrefixes),
  },
}))
