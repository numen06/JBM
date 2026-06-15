import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'landing',
      component: () => import('@/views/landing/LandingPage.vue'),
      meta: { public: true },
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/login/LoginPage.vue'),
      meta: { public: true, authRedirect: true },
    },
    {
      path: '/login/callback',
      name: 'login-callback',
      component: () => import('@/views/login/LoginOAuthCallback.vue'),
      meta: { public: true, authRedirect: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/register/RegisterPage.vue'),
      meta: { public: true },
    },
    {
      path: '/docs',
      name: 'docs',
      component: () => import('@/views/docs/ApiWikiPage.vue'),
      meta: { public: true },
    },
    {
      path: '/docs/openapi/:docKey',
      name: 'published-openapi-doc',
      component: () => import('@/views/docs/PublishedOpenApiPage.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      component: () => import('@/layouts/AdminLayout.vue'),
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/dashboard/DashboardPage.vue'),
          meta: { title: '仪表盘' },
        },
        {
          path: 'messages',
          name: 'messages',
          component: () => import('@/views/messages/MessageCenter.vue'),
          meta: { title: '消息管理' },
        },
        {
          path: 'message-center',
          name: 'message-center',
          component: () => import('@/views/messages/UserMessageCenter.vue'),
          meta: { title: '消息中心' },
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/profile/ProfilePage.vue'),
          meta: { title: '个人中心' },
        },
        {
          path: 'messages/push-test',
          redirect: { name: 'message-send-test' },
        },
        {
          path: 'messages/send-test',
          name: 'message-send-test',
          component: () => import('@/views/messages/PushTestPage.vue'),
          meta: { title: '发送测试' },
        },
        {
          path: 'messages/channels',
          name: 'message-channels',
          component: () => import('@/views/messages/ChannelSettings.vue'),
          meta: { title: '渠道设置' },
        },
        {
          path: 'messages/webhook-configs',
          name: 'webhook-event-configs',
          component: () => import('@/views/messages/WebhookEventConfigList.vue'),
          meta: { title: '事件订阅配置' },
        },
        {
          path: 'messages/webhook-tasks',
          name: 'webhook-tasks',
          component: () => import('@/views/messages/WebhookTaskList.vue'),
          meta: { title: '投递任务' },
        },
        {
          path: 'jobs',
          name: 'jobs',
          component: () => import('@/views/job/JobList.vue'),
          meta: { title: '任务管理' },
        },
        {
          path: 'jobs/logs',
          name: 'job-logs',
          component: () => import('@/views/job/JobLogList.vue'),
          meta: { title: '调度日志' },
        },
        {
          path: 'system/users',
          name: 'users',
          component: () => import('@/views/system/UserList.vue'),
          meta: { title: '用户管理' },
        },
        {
          path: 'system/online-users',
          name: 'online-users',
          component: () => import('@/views/system/OnlineUserList.vue'),
          meta: { title: '在线用户' },
        },
        {
          path: 'system/roles',
          redirect: { name: 'roles' },
        },
        {
          path: 'authority/roles',
          name: 'roles',
          component: () => import('@/views/system/RoleList.vue'),
          meta: { title: '角色管理' },
        },
        {
          path: 'authority/user-permissions',
          name: 'user-permissions',
          component: () => import('@/views/authority/UserPermissionList.vue'),
          meta: { title: '用户权限' },
        },
        {
          path: 'authority/client-permissions',
          name: 'client-permissions',
          component: () => import('@/views/authority/ClientPermissionList.vue'),
          meta: { title: '客户端权限' },
        },
        {
          path: 'authority/catalog',
          name: 'authority-catalog',
          component: () => import('@/views/authority/AuthorityCatalog.vue'),
          meta: { title: '权限目录' },
        },
        {
          path: 'authority/catalog/apis',
          name: 'authority-catalog-apis',
          component: () => import('@/views/authority/AuthorityCatalog.vue'),
          meta: { title: 'API 权限目录' },
        },
        {
          path: 'authority/catalog/pages',
          name: 'authority-catalog-pages',
          component: () => import('@/views/authority/AuthorityCatalog.vue'),
          meta: { title: '页面权限目录' },
        },
        {
          path: 'api/registry',
          name: 'api-registry',
          component: () => import('@/views/api/ApiRegistryList.vue'),
          meta: { title: 'API 资源管理' },
        },
        {
          path: 'api/docs',
          name: 'api-docs',
          component: () => import('@/views/api/ApiDocsPage.vue'),
          meta: { title: 'API 文档与调试' },
        },
        {
          path: 'api/monitor',
          name: 'api-monitor',
          component: () => import('@/views/api/ApiMonitorPage.vue'),
          meta: { title: 'API 监控' },
        },
        {
          path: 'documents',
          name: 'documents',
          component: () => import('@/views/doc/DocumentManagement.vue'),
          meta: { title: '文档管理' },
        },
        {
          path: 'documents/tools',
          name: 'document-tools',
          component: () => import('@/views/doc/DocumentTools.vue'),
          meta: { title: '文档功能区' },
        },
        {
          path: 'system/menus',
          name: 'menus',
          component: () => import('@/views/system/MenuList.vue'),
          meta: { title: '菜单与按钮' },
        },
        {
          path: 'system/actions',
          redirect: { name: 'menus' },
        },
        {
          path: 'system/orgs',
          name: 'orgs',
          component: () => import('@/views/system/OrgList.vue'),
          meta: { title: '组织管理' },
        },
        {
          path: 'system/authorities',
          redirect: { name: 'authority-catalog' },
        },
        {
          path: 'system/authority',
          redirect: { name: 'authority-catalog' },
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
          path: 'system/dicts/:groupId',
          name: 'dict-items',
          component: () => import('@/views/system/DictList.vue'),
          meta: { title: '字典项' },
        },
        {
          path: 'system/extend-fields',
          name: 'extend-fields',
          component: () => import('@/views/system/ExtendFieldList.vue'),
          meta: { title: '扩展字段管理' },
        },
        {
          path: 'system/extend-fields/:formCode',
          name: 'extend-field-detail',
          component: () => import('@/views/system/ExtendFieldList.vue'),
          meta: { title: '扩展字段详情' },
        },
        {
          path: 'gateway/routes',
          name: 'gateway-routes',
          component: () => import('@/views/gateway/RouteList.vue'),
          meta: { title: '路由管理' },
        },
        {
          path: 'gateway/services',
          name: 'gateway-services',
          component: () => import('@/views/gateway/ServiceDiscovery.vue'),
          meta: { title: '服务发现' },
        },
        {
          path: 'gateway/gray-release',
          name: 'gateway-gray',
          component: () => import('@/views/gateway/GrayRelease.vue'),
          meta: { title: '灰度发布' },
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
          path: 'logs',
          redirect: { name: 'log-management' },
        },
        {
          path: 'logs/gateway',
          redirect: { name: 'log-management' },
        },
        {
          path: 'logs/access',
          redirect: { name: 'log-management' },
        },
        {
          path: 'logs/login',
          redirect: { name: 'login-logs' },
        },
        {
          path: 'logs/filter-rules',
          redirect: { name: 'log-filter-rules' },
        },
        {
          path: 'logs/account',
          redirect: { name: 'account-logs' },
        },
        {
          path: 'log/gateway',
          name: 'log-management',
          component: () => import('@/views/log/LogManagement.vue'),
          meta: { title: '访问日志' },
        },
        {
          path: 'log/login',
          name: 'login-logs',
          component: () => import('@/views/log/LogManagement.vue'),
          props: { category: 'login' },
          meta: { title: '登录日志' },
        },
        {
          path: 'log/filter-rules',
          name: 'log-filter-rules',
          component: () => import('@/views/log/LogFilterRules.vue'),
          meta: { title: '采集设置' },
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
        {
          path: 'developer/pending',
          name: 'developer-pending',
          component: () => import('@/views/developer/DeveloperList.vue'),
          meta: { title: '开发者审批' },
        },
        {
          path: 'developer/api-keys',
          name: 'api-keys',
          component: () => import('@/views/developer/ApiKeyList.vue'),
          meta: { title: 'API Key 管理' },
        },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.public) {
    if (auth.isLoggedIn && to.meta.authRedirect) {
      if (!auth.user) {
        const currentUser = await auth.fetchUser()
        if (!currentUser) {
          auth.clearSession()
          return true
        }
      }
      return { name: 'dashboard' }
    }
    return true
  }
  if (!auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (!auth.user) {
    const currentUser = await auth.fetchUser()
    if (!currentUser) {
      auth.clearSession()
      return { name: 'login', query: { redirect: to.fullPath } }
    }
  }
  const menuStore = useMenuStore()
  if (!menuStore.loaded) await menuStore.fetchMenus()
  if (!auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (
    (to.path === '/messages/send-test' ||
      to.path === '/messages/channels' ||
      to.path === '/messages/webhook-configs' ||
      to.path === '/messages/webhook-tasks') &&
    isMessageAdminToolAllowed(auth.user)
  ) {
    return true
  }
  if (to.name === 'profile') {
    return true
  }
  if (!menuStore.isRouteAllowed(to.path)) {
    return { name: 'dashboard' }
  }
  return true
})

function isMessageAdminToolAllowed(user: ReturnType<typeof useAuthStore>['user']) {
  if (!user) return false
  if (user.userId === 1 || user.userName?.toLowerCase() === 'admin') return true
  const authorities = user.authorities?.map((item) => item.authority || item.authorityId || '') ?? []
  return authorities.some((item) =>
    [
      'message_send_test',
      'MENU_message_send_test',
      'message_push_test',
      'MENU_message_push_test',
      'message_channels',
      'MENU_message_channels',
      'push_test',
      'MENU_push_test',
      'push_config',
      'MENU_push_config',
      'webhook_event_config',
      'MENU_webhook_event_config',
      'webhook_tasks',
      'MENU_webhook_tasks',
      'push_webhook',
      'MENU_push_webhook',
      'ACTION_push:test',
      'MENU_push',
    ].includes(item),
  )
}

export default router
