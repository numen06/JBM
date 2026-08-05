import { defineJbmModule, type JbmFrontendModule, type JbmNavigationGroup } from '@jbm7/vue-core'
import { STATIC_NAV_GROUPS } from '@/constants/adminNav'

const VERSION = '7.3.0-beta.1'

function navigation(...labels: string[]): JbmNavigationGroup[] {
  return STATIC_NAV_GROUPS.filter((group) => labels.includes(group.label))
}

export const authModule = defineJbmModule({
  id: 'jbm.auth',
  version: VERSION,
  routes: [
    {
      path: '/__dev/:pathMatch(.*)*',
      name: 'jbm-dev-redirect',
      redirect: (to) => {
        const rest = to.params.pathMatch
        const path = Array.isArray(rest) ? rest.join('/') : String(rest ?? '')
        return path ? `/${path}` : '/'
      },
      meta: { public: true },
    },
    { path: '/', name: 'landing', component: () => import('@/views/landing/LandingPage.vue'), meta: { public: true } },
    { path: '/login', name: 'login', component: () => import('@/views/login/LoginPage.vue'), meta: { public: true, authRedirect: true } },
    { path: '/login/callback', name: 'login-callback', component: () => import('@/views/login/LoginOAuthCallback.vue'), meta: { public: true, authRedirect: true } },
    { path: '/qr-login', name: 'qr-login-confirm', component: () => import('@/views/login/QrLoginConfirmPage.vue'), meta: { public: true } },
    { path: '/register', name: 'register', component: () => import('@/views/register/RegisterPage.vue'), meta: { public: true } },
    { path: '/docs', name: 'docs', component: () => import('@/views/docs/ApiWikiPage.vue'), meta: { public: true } },
    { path: '/docs/openapi/:docKey', name: 'published-openapi-doc', component: () => import('@/views/docs/PublishedOpenApiPage.vue'), meta: { public: true } },
  ],
})

export const coreModule = defineJbmModule({
  id: 'jbm.core',
  version: VERSION,
  navigation: navigation('概览'),
  routes: [
    { path: 'dashboard', name: 'dashboard', component: () => import('@/views/dashboard/DashboardPage.vue'), meta: { title: '仪表盘', menuCode: 'dashboard' } },
    { path: 'profile', name: 'profile', component: () => import('@/views/profile/ProfilePage.vue'), meta: { title: '个人中心' } },
  ],
})

export const systemModule = defineJbmModule({
  id: 'jbm.system',
  version: VERSION,
  navigation: navigation('系统管理'),
  routes: [
    { path: 'system/users', name: 'users', component: () => import('@/views/system/UserList.vue'), meta: { title: '用户管理', menuCode: 'users' } },
    { path: 'system/users/new', name: 'user-new', component: () => import('@/views/system/UserEditPage.vue'), meta: { title: '新建用户', menuCode: 'users' } },
    { path: 'system/users/:userId/edit', name: 'user-edit', component: () => import('@/views/system/UserEditPage.vue'), meta: { title: '编辑用户', menuCode: 'users' } },
    { path: 'system/online-users', name: 'online-users', component: () => import('@/views/system/OnlineUserList.vue'), meta: { title: '在线用户', menuCode: 'onlineUsers' } },
    { path: 'system/orgs', name: 'orgs', component: () => import('@/views/system/OrgList.vue'), meta: { title: '组织管理', menuCode: 'orgs' } },
    { path: 'system/apps', name: 'apps', component: () => import('@/views/system/AppList.vue'), meta: { title: '应用管理', menuCode: 'apps' } },
    { path: 'system/dicts', name: 'dicts', component: () => import('@/views/system/DictList.vue'), meta: { title: '字典管理', menuCode: 'dicts' } },
    { path: 'system/dicts/:groupId', name: 'dict-items', component: () => import('@/views/system/DictList.vue'), meta: { title: '字典项', menuCode: 'dicts' } },
    { path: 'system/extend-fields', name: 'extend-fields', component: () => import('@/views/system/ExtendFieldList.vue'), meta: { title: '扩展字段管理', menuCode: 'extend_fields' } },
    { path: 'system/extend-fields/:formCode', name: 'extend-field-detail', component: () => import('@/views/system/ExtendFieldList.vue'), meta: { title: '扩展字段详情', menuCode: 'extend_fields' } },
  ],
})

export const authorityModule = defineJbmModule({
  id: 'jbm.authority',
  version: VERSION,
  navigation: navigation('权限管理'),
  routes: [
    { path: 'system/roles', name: 'legacy-system-roles', redirect: { name: 'roles' } },
    { path: 'authority/roles', name: 'roles', component: () => import('@/views/system/RoleList.vue'), meta: { title: '角色管理', menuCode: 'roles' } },
    { path: 'authority/user-permissions', name: 'user-permissions', component: () => import('@/views/authority/UserPermissionList.vue'), meta: { title: '用户权限', menuCode: 'user_permissions' } },
    { path: 'authority/client-permissions', name: 'client-permissions', component: () => import('@/views/authority/ClientPermissionList.vue'), meta: { title: '客户端权限', menuCode: 'client_permissions' } },
    { path: 'authority/catalog', name: 'authority-catalog', component: () => import('@/views/authority/AuthorityCatalog.vue'), meta: { title: '权限目录', menuCode: 'authority_catalog' } },
    { path: 'authority/catalog/apis', name: 'authority-catalog-apis', component: () => import('@/views/authority/AuthorityCatalog.vue'), meta: { title: 'API 权限目录', menuCode: 'authority_catalog' } },
    { path: 'authority/catalog/pages', name: 'authority-catalog-pages', component: () => import('@/views/authority/AuthorityCatalog.vue'), meta: { title: '页面权限目录', menuCode: 'authority_catalog' } },
    { path: 'system/menus', name: 'menus', component: () => import('@/views/system/MenuList.vue'), meta: { title: '菜单与按钮', menuCode: 'menus' } },
    { path: 'system/actions', name: 'legacy-system-actions', redirect: { name: 'menus' } },
    { path: 'system/authorities', name: 'legacy-system-authorities', redirect: { name: 'authority-catalog' } },
    { path: 'system/authority', name: 'legacy-system-authority', redirect: { name: 'authority-catalog' } },
  ],
})

export const openapiModule = defineJbmModule({
  id: 'jbm.openapi',
  version: VERSION,
  navigation: navigation('API 管理', '其他'),
  routes: [
    { path: 'api/registry', name: 'api-registry', component: () => import('@/views/api/ApiRegistryList.vue'), meta: { title: 'API 资源管理', menuCode: 'api_registry' } },
    { path: 'api/docs', name: 'api-docs', component: () => import('@/views/api/ApiDocsPage.vue'), meta: { title: 'API 文档与调试', menuCode: 'api_docs' } },
    { path: 'api/monitor', name: 'api-monitor', component: () => import('@/views/api/ApiMonitorPage.vue'), meta: { title: 'API 监控', menuCode: 'api_monitor' } },
    { path: 'developer', name: 'developer', component: () => import('@/views/developer/DeveloperList.vue'), meta: { title: '开发者', menuCode: 'developer' } },
    { path: 'developer/pending', name: 'developer-pending', component: () => import('@/views/developer/DeveloperList.vue'), meta: { title: '开发者审批', menuCode: 'developer' } },
    { path: 'developer/api-keys', name: 'api-keys', component: () => import('@/views/developer/ApiKeyList.vue'), meta: { title: 'API Key 管理', menuCode: 'api_keys' } },
  ],
})

export const gatewayModule = defineJbmModule({
  id: 'jbm.gateway',
  version: VERSION,
  navigation: navigation('网关管理'),
  routes: [
    { path: 'gateway/routes', name: 'gateway-routes', component: () => import('@/views/gateway/RouteList.vue'), meta: { title: '路由管理', menuCode: 'gw_routes' } },
    { path: 'gateway/services', name: 'gateway-services', component: () => import('@/views/gateway/ServiceDiscovery.vue'), meta: { title: '服务发现', menuCode: 'gw_services' } },
    { path: 'gateway/gray-release', name: 'gateway-gray', component: () => import('@/views/gateway/GrayRelease.vue'), meta: { title: '灰度发布', menuCode: 'gw_gray' } },
    { path: 'gateway/rate-limit', name: 'gateway-rate', component: () => import('@/views/gateway/RateLimit.vue'), meta: { title: '限流管理', menuCode: 'gw_rate' } },
    { path: 'gateway/ip-limit', name: 'gateway-ip', component: () => import('@/views/gateway/IpLimit.vue'), meta: { title: 'IP 限制', menuCode: 'gw_ip' } },
  ],
})

export const logsModule = defineJbmModule({
  id: 'jbm.logs',
  version: VERSION,
  navigation: navigation('日志管理'),
  routes: [
    { path: 'logs', name: 'legacy-logs', redirect: { name: 'log-management' } },
    { path: 'logs/gateway', name: 'legacy-logs-gateway', redirect: { name: 'log-management' } },
    { path: 'logs/access', name: 'legacy-logs-access', redirect: { name: 'log-management' } },
    { path: 'logs/login', name: 'legacy-logs-login', redirect: { name: 'login-logs' } },
    { path: 'logs/filter-rules', name: 'legacy-logs-filter-rules', redirect: { name: 'log-filter-rules' } },
    { path: 'logs/account', name: 'legacy-logs-account', redirect: { name: 'account-logs' } },
    { path: 'log/gateway', name: 'log-management', component: () => import('@/views/log/LogManagement.vue'), meta: { title: '访问日志', menuCode: 'gateway_logs' } },
    { path: 'log/login', name: 'login-logs', component: () => import('@/views/log/LogManagement.vue'), props: { category: 'login' }, meta: { title: '登录日志', menuCode: 'login_logs' } },
    { path: 'log/filter-rules', name: 'log-filter-rules', component: () => import('@/views/log/LogFilterRules.vue'), meta: { title: '采集设置', menuCode: 'log_filter_rules' } },
    { path: 'log/account', name: 'account-logs', component: () => import('@/views/log/AccountLogs.vue'), meta: { title: '审计日志', menuCode: 'account_logs' } },
  ],
})

export const messagesModule = defineJbmModule({
  id: 'jbm.messages',
  version: VERSION,
  navigation: navigation('消息管理'),
  routes: [
    { path: 'messages', name: 'messages', component: () => import('@/views/messages/MessageCenter.vue'), meta: { title: '消息管理', menuCode: 'messages' } },
    { path: 'message-center', name: 'message-center', component: () => import('@/views/messages/UserMessageCenter.vue'), meta: { title: '消息中心' } },
    { path: 'messages/push-test', name: 'legacy-message-push-test', redirect: { name: 'message-send-test' } },
    { path: 'messages/send-test', name: 'message-send-test', component: () => import('@/views/messages/PushTestPage.vue'), meta: { title: '发送测试', menuCode: 'message_send_test' } },
    { path: 'messages/channels', name: 'message-channels', component: () => import('@/views/messages/ChannelSettings.vue'), meta: { title: '渠道设置', menuCode: 'message_channels' } },
    { path: 'messages/webhook-configs', name: 'webhook-event-configs', component: () => import('@/views/messages/WebhookEventConfigList.vue'), meta: { title: '事件订阅配置', menuCode: 'webhook_event_config' } },
    { path: 'messages/webhook-tasks', name: 'webhook-tasks', component: () => import('@/views/messages/WebhookTaskList.vue'), meta: { title: '投递任务', menuCode: 'webhook_tasks' } },
  ],
})

export const jobsModule = defineJbmModule({
  id: 'jbm.jobs',
  version: VERSION,
  navigation: navigation('任务调度'),
  routes: [
    { path: 'jobs', name: 'jobs', component: () => import('@/views/job/JobList.vue'), meta: { title: '任务管理', menuCode: 'jobs' } },
    { path: 'jobs/logs', name: 'job-logs', component: () => import('@/views/job/JobLogList.vue'), meta: { title: '调度日志', menuCode: 'job_logs' } },
  ],
})

export const documentsModule = defineJbmModule({
  id: 'jbm.documents',
  version: VERSION,
  navigation: navigation('文档管理'),
  routes: [
    { path: 'documents', name: 'documents', component: () => import('@/views/doc/DocumentManagement.vue'), meta: { title: '文档管理', menuCode: 'documents' } },
    { path: 'documents/tools', name: 'document-tools', component: () => import('@/views/doc/DocumentTools.vue'), meta: { title: '文档功能区', menuCode: 'documents' } },
  ],
})

export const adminChildModules: JbmFrontendModule[] = [
  coreModule,
  systemModule,
  authorityModule,
  openapiModule,
  gatewayModule,
  messagesModule,
  jobsModule,
  documentsModule,
  logsModule,
]

export const adminModules: JbmFrontendModule[] = [authModule, ...adminChildModules]

export const adminHostRoutes = [
  ...authModule.routes,
  {
    path: '/',
    name: 'jbm-admin-shell',
    component: () => import('@/layouts/AdminLayout.vue'),
    children: [
      ...adminChildModules.flatMap((module) => module.routes),
      { path: ':pathMatch(.*)*', name: 'admin-not-found', redirect: { name: 'dashboard' } },
    ],
  },
]
