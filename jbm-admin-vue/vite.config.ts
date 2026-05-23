import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import path from 'node:path'

/** jaja7 本地：所有 API 经 Gateway 7777（需同时启动 Gateway / Auth / Center） */
const gateway = 'http://127.0.0.1:7777'

const gatewayPrefixes = [
  '/oauth2',
  '/captcha',
  '/qrcode',
  '/user',
  '/current',
  '/authority',
  '/role',
  '/menu',
  '/account',
  '/online',
  '/statistics',
  '/internal',
  '/app',
  '/gateway',
  '/baseDic',
  '/developer',
  '/baseOrg',
  '/baseAccountLogs',
  '/action',
  '/api',
  '/baseAuthorityAction',
  '/baseAuthorityRole',
  '/baseAuthorityUser',
  '/baseAuthorityApp',
  '/gatewayRateLimitApi',
  '/gatewayIpLimitApi',
  '/extend-field',
]

function proxyMap(target: string, prefixes: string[]) {
  return Object.fromEntries(
    prefixes.map((p) => [p, { target, changeOrigin: true }]),
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
    proxy: proxyMap(gateway, gatewayPrefixes),
  },
})
