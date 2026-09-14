import { describe, expect, it } from 'vitest'
import Verify from './Verify.js'

describe('admin input validation', () => {
  it('validates account passwords', () => {
    expect(Verify.checkPassword('Admin2026!')).toBe(true)
    expect(Verify.checkPassword('weak')).toBe(false)
  })

  it('validates email and positive integer input', () => {
    expect(Verify.checkEmail('ops@zentide.test')).toBe(true)
    expect(Verify.checkEmail('invalid')).toBe(false)
    expect(Verify.checkNumber('12')).toBe(true)
    expect(Verify.checkNumber('0')).toBe(false)
  })
})
