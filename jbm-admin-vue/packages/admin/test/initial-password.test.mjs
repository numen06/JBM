import assert from 'node:assert/strict'
import test from 'node:test'
import { generateInitialPassword, passwordPolicyError } from '../../../src/lib/passwordPolicy.ts'

test('initial passwords have ten characters and meet the account password policy', () => {
  const passwords = new Set(Array.from({ length: 100 }, () => generateInitialPassword()))
  assert.equal(passwords.size, 100)
  for (const password of passwords) {
    assert.equal(password.length, 10)
    assert.equal(passwordPolicyError(password), '')
    assert.match(password, /^[A-Za-z2-9]+$/)
  }
})
