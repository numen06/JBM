import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import path from 'node:path'

const gateway = 'http://127.0.0.1:7777'
const auth = 'http://127.0.0.1:5555'
const center = 'http://127.0.0.1:8888'

/** jaja7 Gateway 已路由的前缀 */
const gatewayPrefixes = [
  '/oauth2',
  '/user',
  '/current',
  '/authority',
  '/role',
  '/menu',
  '/account',
  '/online',
  '/statistics',
  '/internal',
]

/** 直连 Center（Gateway jaja7 未配置的路由） */
const centerPrefixes = [
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
    proxy: {
      ...proxyMap(gateway, gatewayPrefixes),
      ...proxyMap(center, centerPrefixes),
      /** 验证码在 Auth，jaja7 Gateway 未路由 /captcha */
      '/captcha': { target: auth, changeOrigin: true },
    },
  },
})
