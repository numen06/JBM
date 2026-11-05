export type AuditScope = 'auto' | 'current'

export function auditQueryBody(body: Record<string, unknown>, scope: AuditScope, activeTenantId: string) {
  if (scope !== 'current') return body
  if (!/^[1-9]\d*$/.test(activeTenantId)) throw new Error('请先选择项目，再查看审计日志。')
  return { ...body, tenantId: activeTenantId, tenantScopeVerified: true }
}
