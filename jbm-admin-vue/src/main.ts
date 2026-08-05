import './hostRuntimeConfig'
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { createJbmVuePlugin } from '@jbm7/vue-core'
import App from './App.vue'
import router from './router'
import { createAdminPlatformClient } from '@/platformClient'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import './assets/index.css'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)

const client = createAdminPlatformClient(router)
app.use(createJbmVuePlugin({
  client,
  access: {
    isAuthenticated: () => useAuthStore().isLoggedIn,
    hasMenu: (code) => useMenuStore().allowedMenuCodes.has(code),
    hasPermission: (code) => useAuthStore().user?.authorities?.some((item) =>
      item.authority === code || item.authorityId === code,
    ) ?? false,
  },
}))
app.use(router)
app.mount('#app')
