import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import path from 'node:path'

const entries = [
  'index', 'auth', 'system', 'authority', 'gateway', 'logs',
  'messages', 'jobs', 'documents', 'openapi', 'shell', 'host',
  'bigscreen',
]

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, '../../src'),
      '@jbm7/sdk': path.resolve(__dirname, '../sdk/src/index.ts'),
      '@jbm7/vue-core': path.resolve(__dirname, '../vue-core/src/index.ts'),
    },
  },
  build: {
    cssCodeSplit: false,
    lib: {
      entry: Object.fromEntries(entries.map((name) => [name, path.resolve(__dirname, `src/${name}.ts`)])),
      formats: ['es'],
      cssFileName: 'style',
    },
    rollupOptions: {
      external: ['vue', 'vue-router', 'pinia', '@jbm7/sdk', '@jbm7/vue-core'],
      output: {
        entryFileNames: '[name].js',
        chunkFileNames: 'chunks/[name]-[hash].js',
        assetFileNames: (asset) => asset.name?.endsWith('.css') ? 'style.css' : 'assets/[name]-[hash][extname]',
      },
    },
    sourcemap: false,
  },
})
