import assert from 'node:assert/strict'
import test from 'node:test'
import { canAdministerTenantMembers, isPlatformAdministrator, isSuperAdministrator, isPlatformAdminPath, platformMenuCodes } from '../../../src/lib/adminAccess.ts'

test('built-in tenant member actions require an owner administrator in its own tenant', () => {
  const user = { roles: [{ roleCode: 'tenant_admin' }] }
  assert.equal(canAdministerTenantMembers(user, '1', '1'), true)
  assert.equal(canAdministerTenantMembers(user, '1', '2000'), false)
  assert.equal(canAdministerTenantMembers(user, '0', '0'), false)
  assert.equal(canAdministerTenantMembers({ roles: [{ roleCode: 'operations' }] }, '1', '1'), false)
  assert.equal(canAdministerTenantMembers({ roles: [{ roleCode: 'iot_admin' }] }, '1', '1'), false)
  assert.equal(canAdministerTenantMembers(null, '1', '1'), false)
})

test('a numeric role ID cannot turn a tenant account into a platform administrator', () => {
  const tenant = { userName: 'tenant_admin', roles: [{ roleId: 1, roleCode: 'tenant_admin' }] }
  assert.equal(isSuperAdministrator(tenant), false)
  assert.equal(isPlatformAdministrator(tenant), false)
})

test('verified platform roles retain administration access', () => {
  assert.equal(isPlatformAdministrator({ roles: [{ roleCode: 'platform_operator' }] }), true)
  assert.equal(isSuperAdministrator({ roles: [{ roleCode: 'super_admin' }] }), true)
  assert.equal(isPlatformAdministrator(null), false)
})

test('platform governance routes are restricted while business and member routes remain available', () => {
  for (const path of ['/system/apps', '/jbm/system/apps/1', '/system/database-backups', '/jbm/system/database-backups', '/api/docs', '/authority/roles', '/gateway/routes']) {
    assert.equal(isPlatformAdminPath(path), true)
  }
  for (const path of ['/dashboard', '/system/users', '/documents', '/building-energy', '/iot/console']) {
    assert.equal(isPlatformAdminPath(path), false)
  }
})

test('database maintenance is a platform permission', () => {
  assert.equal(platformMenuCodes.has('database_backups'), true)
})
