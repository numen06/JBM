type Event = {
  serviceId?: string; target?: string; operation?: string; operationName?: string;
  resourceIds?: unknown; submittedChanges?: unknown; businessRef?: Record<string, unknown>;
}
const services: Record<string, string> = {
  'smart-building-app': '建筑平台', 'smart-building-batch': '建筑批处理', 'smart-building-data-batch': '建筑批处理',
  'iot-platform': '物联网', 'tpm-iot-platform': '物联网',
  'jbm-cluster-platform-center': '系统管理', 'jbm-cluster-platform-auth': '登录认证',
  'jbm-cluster-platform-gateway': '网关管理', 'jbm-cluster-platform-job': '任务调度',
  'jbm-cluster-platform-doc': '文件管理', 'jbm-cluster-platform-logs': '日志管理',
}
const resources: Record<string, string> = {
  projects: '项目', project: '项目', structures: '建筑结构', structure: '建筑结构',
  'structure-types': '结构类型', devices: '设备', device: '设备', gateways: '网关',
  products: '产品', policies: '策略', rules: '规则', users: '用户', user: '用户',
  roles: '角色', role: '角色', reports: '报表', 'energy-types': '能源类型',
  'energy-prices': '能源价格', drawings: '图纸', schedules: '计划', tasks: '任务',
  baseProject: '项目', baseStructure: '建筑结构', baseEnergy: '能源档案',
}
const fields: Record<string, string> = {
  name: '名称', title: '标题', code: '编码', projectName: '项目名称', structureName: '结构名称',
  deviceName: '设备名称', gatewayName: '网关名称', productName: '产品名称', nickName: '昵称',
  status: '状态', enabled: '启用', type: '类型', value: '值', description: '说明',
}
const hidden = /password|secret|token|private|credential|signature|authorization/i
function object(value: unknown): Record<string, unknown> {
  return value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : {}
}
function body(event: Event) {
  const root = object(event.submittedChanges)
  return { ...root, ...object(root.data), ...object(root.form) }
}
function text(value: unknown) {
  return typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean' ? String(value).slice(0, 100) : ''
}
export function auditModule(event: Event) { return services[event.serviceId || ''] || event.serviceId || '业务系统' }
export function auditObject(event: Event) {
  const resource = (event.target || '').split('/').map(part => resources[part]).reverse().find(Boolean) || ''
  const values = body(event)
  const name = ['projectName', 'structureName', 'deviceName', 'gatewayName', 'productName', 'name', 'title', 'code'].map(key => text(values[key])).find(Boolean)
  const ids = Object.entries(object(event.resourceIds)).filter(([key]) => !hidden.test(key) && key !== 'resource').map(([, value]) => text(value)).filter(Boolean)
  return [resource, name || ids.join(' / ')].filter(Boolean).join(' · ') || event.target || '身份认证'
}
export function auditChanges(event: Event) {
  return Object.entries(body(event)).filter(([key, value]) => !hidden.test(key) && text(value) && !String(value).includes('[已脱敏]'))
    .slice(0, 3).map(([key, value]) => `${fields[key] || key}：${text(value)}`).join('；')
}
export function auditAction(event: Event) {
  const values = body(event)
  if (values.type === 'gateway.check') return '检查网关'
  return event.operationName || ({ POST: '提交', PUT: '修改', PATCH: '修改', DELETE: '删除', GET: '读取' } as Record<string,string>)[event.operation || ''] || event.operation || '业务操作'
}
