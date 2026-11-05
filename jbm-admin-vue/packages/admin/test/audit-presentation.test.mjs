import test from 'node:test'
import assert from 'node:assert/strict'
import { auditModule, auditObject, auditChanges, auditAction } from '../../../src/lib/auditPresentation.ts'
test('business audit shows supplied object identity, submitted fields and actual operation', () => {
  const event = { serviceId:'smart-building-app',target:'/api/v1/building/master-data/structures/42',resourceIds:{resource:'structures',record_id:'42'},submittedChanges:{name:'教学楼',status:0},operationName:'修改业务资料' }
  assert.equal(auditModule(event),'建筑平台')
  assert.equal(auditObject(event),'建筑结构 · 教学楼')
  assert.equal(auditChanges(event),'名称：教学楼；状态：0')
  assert.equal(auditAction(event),'修改建筑结构')
  assert.equal(auditAction({...event,operation:'POST'}),'新增建筑结构')
  assert.equal(auditObject({...event,submittedChanges:undefined,operationSummary:{submittedChanges:{name:'教学楼'}}}),'建筑结构 · 教学楼')
  assert.equal(auditObject({...event,submittedChanges:{}}),'建筑结构 · 42')
  assert.equal(auditObject({target:'/api/v1/building/master-data/structure-types/52',submittedChanges:{structureTypeName:'楼宇'}}),'结构类型 · 楼宇')
})
test('audit summaries never expose credentials or invent missing values', () => {
  assert.equal(auditChanges({submittedChanges:{password:'x',access_token:'y',privateKey:'z',name:'[已脱敏]'}}),'')
  assert.equal(auditObject({target:'/unknown/42'}),'/unknown/42')
  assert.equal(auditAction({submittedChanges:{type:'gateway.check'}}),'检查网关')
  assert.equal(auditAction({target:'/api/v1/building/energy-assistant/selection',operation:'PUT',operationName:'修改接口权限'}),'修改模型选择')
  assert.equal(auditAction({target:'/tenant-features/catalog',operation:'POST'}),'新增功能定义')
  assert.equal(auditModule({serviceId:'jbm-cluster-platform-center-jbm7'}),'系统管理')
})
