import type { JbmClient } from '@jbm7/sdk'

export interface JbmTenantOption {
  value: string
  label: string
  name?: string
  code?: string
  source: 'current' | 'platform' | 'delegated'
}

interface FeatureTenant {
  tenantId?: string | number
  tenantName?: string
  tenantCode?: string
}

interface ReceivedDelegation {
  ownerTenantId?: string | number
  ownerTenantName?: string
  purpose?: string
}

function tenantLabel(option: Omit<JbmTenantOption, 'label'>, purpose = '') {
  const identity = option.name
    ? `${option.name}${option.code ? `（${option.code}）` : ''}`
    : option.code || option.value
  if (option.source === 'current') return `本方租户 · ${identity}`
  if (option.source === 'delegated') return `受托租户 · ${identity}${purpose ? ` · ${purpose}` : ''}`
  return `租户 · ${identity}`
}

export function mergeJbmTenantOptions(
  currentTenantId: string,
  currentTenantName = '',
  platformTenants: FeatureTenant[] = [],
  delegations: ReceivedDelegation[] = [],
): JbmTenantOption[] {
  const current = currentTenantId || '0'
  const options = new Map<string, JbmTenantOption>()
  const put = (item: Omit<JbmTenantOption, 'label'>, purpose = '') => {
    if (!item.value || options.has(item.value)) return
    options.set(item.value, { ...item, label: tenantLabel(item, purpose) })
  }

  const matchingCurrent = platformTenants.find(item => String(item.tenantId ?? '') === current)
  put({
    value: current,
    name: matchingCurrent?.tenantName || currentTenantName || undefined,
    code: matchingCurrent?.tenantCode || undefined,
    source: 'current',
  })
  for (const item of platformTenants) {
    const value = String(item.tenantId ?? '')
    if (value === current) continue
    put({ value, name: item.tenantName, code: item.tenantCode, source: 'platform' })
  }
  for (const item of delegations) {
    const value = String(item.ownerTenantId ?? '')
    if (value === current) continue
    put({ value, name: item.ownerTenantName, source: 'delegated' }, item.purpose || '委托运营')
  }
  return [...options.values()]
}

export function createJbmTenantDirectory(
  client: JbmClient,
  identity: () => { tenantId: string; tenantName?: string },
) {
  return {
    async listAvailable(): Promise<JbmTenantOption[]> {
      const current = identity()
      const [contextResult, delegationResult] = await Promise.allSettled([
        client.get<{ result?: { tenants?: FeatureTenant[] } }>('/center/tenant-features/context'),
        client.get<{ result?: { contents?: ReceivedDelegation[] } }>('/center/tenant-delegation/received', {
          params: { 'pageForm.currPage': 1, 'pageForm.pageSize': 100 },
        }),
      ])
      const platformTenants = contextResult.status === 'fulfilled'
        ? contextResult.value.result?.tenants ?? []
        : []
      const delegations = delegationResult.status === 'fulfilled'
        ? delegationResult.value.result?.contents ?? []
        : []
      return mergeJbmTenantOptions(current.tenantId, current.tenantName, platformTenants, delegations)
    },
  }
}
