import {
  computed,
  defineComponent,
  h,
  inject,
  ref,
  watch,
  type App,
  type Component,
  type InjectionKey,
  type Plugin,
  type PropType,
} from 'vue'
import type {
  NavigationGuard,
  RouteLocationNormalized,
  RouteRecordRaw,
  Router,
} from 'vue-router'
import type { JbmClient } from '@jbm7/sdk'

export interface JbmRouteMeta {
  title?: string
  menuCode?: string
  permissions?: string[]
  public?: boolean
  authRedirect?: boolean
}

export type JbmRoute = RouteRecordRaw & { meta?: JbmRouteMeta }

export interface JbmNavigationItem {
  name: string
  title: string
  to: string
  icon?: Component
  menuCodes?: string[]
  permissions?: string[]
}

export interface JbmNavigationGroup {
  label: string
  items: JbmNavigationItem[]
}

export interface JbmShellWorkspace {
  key: string
  label: string
  shortLabel?: string
  path: string
  icon?: Component
}

export interface JbmShellNavigationItem {
  path: string
  label: string
  icon?: Component
  active?: boolean
}

export interface JbmShellNavigationGroup {
  label: string
  hint?: string
  items: JbmShellNavigationItem[]
}

const navigateEmits = { navigate: (_path: string) => true }

export const JbmWorkspaceNavigation = defineComponent({
  name: 'JbmWorkspaceNavigation',
  props: {
    workspaces: { type: Array as PropType<JbmShellWorkspace[]>, required: true },
    currentKey: { type: String, required: true },
  },
  emits: navigateEmits,
  setup(props, { emit }) {
    return () => h('div', { class: 'jbm-workspaces' }, [
      h('div', { class: 'jbm-workspaces__label' }, '应用工作区'),
      h('div', { class: 'jbm-workspaces__grid' }, props.workspaces.map(item => h('a', {
        href: item.path,
        title: item.label,
        class: ['jbm-workspaces__item', item.key === props.currentKey && 'is-active'],
        onClick: (event: MouseEvent) => { event.preventDefault(); emit('navigate', item.path) },
      }, [
        item.icon ? h(item.icon, { class: 'jbm-workspaces__icon', 'aria-hidden': 'true' }) : null,
        h('span', item.shortLabel || item.label),
      ]))),
    ])
  },
})

export const JbmSidebarNavigation = defineComponent({
  name: 'JbmSidebarNavigation',
  props: {
    groups: { type: Array as PropType<JbmShellNavigationGroup[]>, required: true },
  },
  emits: navigateEmits,
  setup(props, { emit }) {
    const openLabels = ref<string[]>([])
    const activePath = computed(() => props.groups
      .flatMap(group => group.items)
      .filter(item => item.active)
      .reduce((selected, item) => item.path.length > selected.length ? item.path : selected, ''))
    const isActive = (item: JbmShellNavigationItem) => item.active && item.path === activePath.value
    const activeLabels = computed(() => props.groups.filter(group => group.items.some(isActive)).map(group => group.label))
    watch([() => props.groups, activeLabels], ([groups, active]) => {
      const valid = new Set(groups.map(group => group.label))
      openLabels.value = [...new Set([
        ...openLabels.value.filter(label => valid.has(label)),
        ...active,
        ...(openLabels.value.length || !groups[0] ? [] : [groups[0].label]),
      ])]
    }, { immediate: true })
    const isOpen = (label: string) => openLabels.value.includes(label)
    const toggle = (label: string) => {
      openLabels.value = isOpen(label) ? openLabels.value.filter(item => item !== label) : [...openLabels.value, label]
    }
    return () => h('nav', { class: 'jbm-sidebar-nav', 'aria-label': '主菜单' }, props.groups.map(group => h('section', {
      class: 'jbm-sidebar-nav__group',
    }, [
      h('button', {
        type: 'button',
        class: ['jbm-sidebar-nav__group-button', group.items.some(isActive) && 'is-active'],
        'aria-expanded': isOpen(group.label),
        onClick: () => toggle(group.label),
      }, [
        h('span', [h('strong', group.label), group.hint ? h('small', group.hint) : null]),
        h('span', { class: ['jbm-sidebar-nav__chevron', isOpen(group.label) && 'is-open'], 'aria-hidden': 'true' }, '›'),
      ]),
      isOpen(group.label) ? h('div', { class: 'jbm-sidebar-nav__items' }, group.items.map(item => h('a', {
        href: item.path,
        class: ['jbm-sidebar-nav__item', isActive(item) && 'is-active'],
        'aria-current': isActive(item) ? 'page' : undefined,
        onClick: (event: MouseEvent) => { event.preventDefault(); emit('navigate', item.path) },
      }, [
        h('span', { class: 'jbm-sidebar-nav__icon' }, item.icon ? [h(item.icon, { 'aria-hidden': 'true' })] : []),
        h('span', { class: 'jbm-sidebar-nav__text' }, item.label),
      ]))) : null,
    ])))
  },
})

export interface JbmFrontendModule {
  id: string
  version: string
  routes: JbmRoute[]
  navigation?: JbmNavigationGroup[]
}

export interface JbmAccessProvider {
  isAuthenticated(): boolean
  hasMenu(menuCode: string): boolean
  hasPermission(permission: string): boolean
}

export interface JbmVuePluginOptions {
  client: JbmClient
  access?: JbmAccessProvider
}

export interface RegisterJbmModulesOptions {
  router: Router
  modules: JbmFrontendModule[]
  parentRouteName?: string
  onDiagnostic?: (message: string) => void
}

export interface JbmModuleRegistry {
  readonly modules: readonly JbmFrontendModule[]
  navigation(authorizedMenus: ReadonlySet<string>, permissions?: ReadonlySet<string>): JbmNavigationGroup[]
  unknownAuthorizedMenus(authorizedMenus: ReadonlySet<string>): string[]
}

export const JBM_CLIENT_KEY: InjectionKey<JbmClient> = Symbol('jbm.client')
export const JBM_ACCESS_KEY: InjectionKey<JbmAccessProvider> = Symbol('jbm.access')

export function createJbmVuePlugin(options: JbmVuePluginOptions): Plugin {
  return {
    install(app: App) {
      app.provide(JBM_CLIENT_KEY, options.client)
      if (options.access) app.provide(JBM_ACCESS_KEY, options.access)
    },
  }
}

export function useJbmClient() {
  const client = inject(JBM_CLIENT_KEY)
  if (!client) throw new Error('JBM client is not installed')
  return client
}

export function useJbmAccess() {
  return inject(JBM_ACCESS_KEY)
}

export function defineJbmModule(definition: JbmFrontendModule): JbmFrontendModule {
  if (!/^[a-z0-9]+(?:[.-][a-z0-9]+)+$/.test(definition.id)) {
    throw new Error(`Invalid JBM module id: ${definition.id}. Use a namespaced id such as jbm.gateway.`)
  }
  if (!definition.version.trim()) throw new Error(`JBM module ${definition.id} requires a version`)
  return definition
}

export function registerJbmModules(options: RegisterJbmModulesOptions): JbmModuleRegistry {
  const moduleIds = new Set<string>()
  const routeNames = new Set(options.router.getRoutes().map((route) => String(route.name ?? '')).filter(Boolean))
  const routePaths = new Set(options.router.getRoutes().map((route) => route.path))

  for (const module of options.modules) {
    defineJbmModule(module)
    if (moduleIds.has(module.id)) throw new Error(`Duplicate JBM module id: ${module.id}`)
    moduleIds.add(module.id)
    for (const route of module.routes) {
      validateRoute(module.id, route, routeNames, routePaths)
      if (options.parentRouteName) options.router.addRoute(options.parentRouteName, route)
      else options.router.addRoute(route)
    }
  }

  const registry: JbmModuleRegistry = {
    modules: Object.freeze([...options.modules]),
    navigation: (authorizedMenus, permissions = new Set()) => buildNavigation(options.modules, authorizedMenus, permissions),
    unknownAuthorizedMenus: (authorizedMenus) => {
      const known = collectMenuCodes(options.modules)
      const unknown = [...authorizedMenus].filter((menu) => !known.has(menu))
      if (unknown.length) options.onDiagnostic?.(`Authorized menus are not installed: ${unknown.join(', ')}`)
      return unknown
    },
  }
  return registry
}

export function createJbmRouteGuard(access: JbmAccessProvider, loginRouteName = 'login'): NavigationGuard {
  return (to: RouteLocationNormalized) => {
    const meta = to.meta as JbmRouteMeta
    if (meta.public) return true
    if (!access.isAuthenticated()) {
      return { name: loginRouteName, query: { redirect: to.fullPath } }
    }
    if (meta.menuCode && !access.hasMenu(meta.menuCode)) return false
    if (meta.permissions?.some((permission) => !access.hasPermission(permission))) return false
    return true
  }
}

function validateRoute(
  moduleId: string,
  route: JbmRoute,
  routeNames: Set<string>,
  routePaths: Set<string>,
) {
  const name = String(route.name ?? '')
  if (!name) throw new Error(`JBM module ${moduleId} has a route without a name`)
  if (routeNames.has(name)) throw new Error(`Duplicate JBM route name: ${name}`)
  if (routePaths.has(route.path)) throw new Error(`Duplicate JBM route path: ${route.path}`)
  routeNames.add(name)
  routePaths.add(route.path)
}

function buildNavigation(
  modules: JbmFrontendModule[],
  authorizedMenus: ReadonlySet<string>,
  permissions: ReadonlySet<string>,
) {
  return modules.flatMap((module) =>
    (module.navigation ?? []).flatMap((group) => {
      const items = group.items.filter((item) => {
        const menuAllowed = !item.menuCodes?.length || item.menuCodes.some((code) => authorizedMenus.has(code))
        const permissionsAllowed = !item.permissions?.length || item.permissions.every((code) => permissions.has(code))
        return menuAllowed && permissionsAllowed
      })
      return items.length ? [{ ...group, items }] : []
    }),
  )
}

function collectMenuCodes(modules: JbmFrontendModule[]) {
  const codes = new Set<string>()
  for (const module of modules) {
    for (const route of module.routes) if (route.meta?.menuCode) codes.add(route.meta.menuCode)
    for (const group of module.navigation ?? []) {
      for (const item of group.items) for (const code of item.menuCodes ?? []) codes.add(code)
    }
  }
  return codes
}

declare module 'vue-router' {
  interface RouteMeta extends JbmRouteMeta {}
}
