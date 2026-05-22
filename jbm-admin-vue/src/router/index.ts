import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/login/LoginPage.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      component: () => import('@/layouts/AdminLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/dashboard/DashboardPage.vue'),
          meta: { title: '仪表盘' },
        },
        {
          path: 'system/users',
          name: 'users',
          component: () => import('@/views/system/UserList.vue'),
          meta: { title: '用户管理' },
        },
        {
          path: 'system/roles',
          name: 'roles',
          component: () => import('@/views/system/RoleList.vue'),
          meta: { title: '角色管理' },
        },
        {
          path: 'system/menus',
          name: 'menus',
          component: () => import('@/views/system/MenuList.vue'),
          meta: { title: '菜单管理' },
        },
        {
          path: 'system/orgs',
          name: 'orgs',
          component: () => import('@/views/system/OrgList.vue'),
          meta: { title: '组织管理' },
        },
        {
          path: 'system/authorities',
          name: 'authorities',
          component: () => import('@/views/system/AuthorityList.vue'),
          meta: { title: '权限管理' },
        },
        {
          path: 'system/apps',
          name: 'apps',
          component: () => import('@/views/system/AppList.vue'),
          meta: { title: '应用管理' },
        },
        {
          path: 'system/dicts',
          name: 'dicts',
          component: () => import('@/views/system/DictList.vue'),
          meta: { title: '字典管理' },
        },
        {
          path: 'gateway/routes',
          name: 'gateway-routes',
          component: () => import('@/views/gateway/RouteList.vue'),
          meta: { title: '路由管理' },
        },
        {
          path: 'gateway/rate-limit',
          name: 'gateway-rate',
          component: () => import('@/views/gateway/RateLimit.vue'),
          meta: { title: '限流管理' },
        },
        {
          path: 'gateway/ip-limit',
          name: 'gateway-ip',
          component: () => import('@/views/gateway/IpLimit.vue'),
          meta: { title: 'IP 限制' },
        },
        {
          path: 'log/account',
          name: 'account-logs',
          component: () => import('@/views/log/AccountLogs.vue'),
          meta: { title: '审计日志' },
        },
        {
          path: 'developer',
          name: 'developer',
          component: () => import('@/views/developer/DeveloperList.vue'),
          meta: { title: '开发者' },
        },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.public) {
    if (auth.isLoggedIn && to.name === 'login') return { name: 'dashboard' }
    return true
  }
  if (!auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (!auth.user) await auth.fetchUser()
  return true
})

export default router
