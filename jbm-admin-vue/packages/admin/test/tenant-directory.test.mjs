import assert from 'node:assert/strict'
import test from 'node:test'
import { createJbmTenantDirectory, mergeJbmTenantOptions } from '../src/tenants.ts'

test('JBM tenant directory merges platform and delegated tenants without duplicates', () => {
  assert.deepEqual(
    mergeJbmTenantOptions(
      '1',
      '',
      [
        { tenantId: '1', tenantName: '平台运营方', tenantCode: 'platform' },
        { tenantId: '2', tenantName: '建筑一租户', tenantCode: 'building-a' },
      ],
      [
        { ownerTenantId: '2', purpose: '重复委托' },
        { ownerTenantId: '3', ownerTenantName: '建筑二租户', purpose: '能源托管' },
      ],
    ),
    [
      { value: '1', name: '平台运营方', code: 'platform', source: 'current', label: '本方租户 · 平台运营方（platform）' },
      { value: '2', name: '建筑一租户', code: 'building-a', source: 'platform', label: '租户 · 建筑一租户（building-a）' },
      { value: '3', name: '建筑二租户', source: 'delegated', label: '受托租户 · 建筑二租户 · 能源托管' },
    ],
  )
})

test('JBM tenant directory always keeps the current tenant as a safe fallback', () => {
  assert.deepEqual(mergeJbmTenantOptions('9'), [
    { value: '9', name: undefined, code: undefined, source: 'current', label: '本方租户 · 9' },
  ])
})

test('platform administrators can switch across global root tenants', async () => {
  const client = {
    async get(path) {
      if (path === '/center/tenant-features/context') return { result: { platform: true, tenants: [] } }
      return { result: { contents: [] } }
    },
    async post() {
      return { result: [
        { id: '1', orgName: '平台运营方', orgCode: 'platform' },
        { id: '2', orgName: '建筑租户', orgCode: 'building' },
      ] }
    },
  }
  assert.deepEqual(
    await createJbmTenantDirectory(client, () => ({ tenantId: '1' })).listAvailable(),
    [
      { value: '1', name: '平台运营方', code: 'platform', source: 'current', label: '本方租户 · 平台运营方（platform）' },
      { value: '2', name: '建筑租户', code: 'building', source: 'platform', label: '租户 · 建筑租户（building）' },
    ],
  )
})
