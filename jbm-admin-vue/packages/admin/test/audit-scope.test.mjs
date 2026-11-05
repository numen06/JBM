import assert from 'node:assert/strict'
import test from 'node:test'
import { auditQueryBody } from '../../../src/lib/auditScope.ts'

test('current audit scope pins list, operation and task queries to the selected verified scope', () => {
  for (const body of [{ page: 1, size: 20 }, { operationId: 'op-1' }, { businessTaskIds: ['task-1'] }]) {
    assert.deepEqual(auditQueryBody({ ...body, tenantId: '999', tenantScopeVerified: false }, 'current', '1'), {
      ...body, tenantId: '1', tenantScopeVerified: true,
    })
  }
})
test('missing or invalid current scope never becomes a global query', () => {
  for (const value of ['', '0', '-1', '1 OR 1', '1.0']) assert.throws(() => auditQueryBody({}, 'current', value))
})
test('global audit retains existing explicit filters', () => {
  const body = { tenantId: '2', category: 'login' }
  assert.deepEqual(auditQueryBody(body, 'auto', '1'), body)
  assert.deepEqual(auditQueryBody({}, 'auto', '0'), {})
})
