import { defineConfig } from 'vite'

const gatewayPrefixes = ['/v3/api', '/auth', '/center', '/push', '/jbm-cluster-platform-']

export default defineConfig({
  server: {
    proxy: Object.fromEntries(gatewayPrefixes.map((path) => [path, {
      target: process.env.JBM_GATEWAY_URL || 'http://127.0.0.1:6060',
      changeOrigin: true,
      ws: true,
    }])),
  },
})
