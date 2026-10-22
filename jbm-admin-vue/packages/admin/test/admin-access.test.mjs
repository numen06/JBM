import assert from 'node:assert/strict'
import test from 'node:test'
import { isPlatformAdministrator, isSuperAdministrator, isPlatformAdminPath, platformMenuCodes } from '../../../src/lib/adminAccess.ts'

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
