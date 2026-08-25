import { del, get, post, put, unwrap } from './request'

export interface AppFeature {
  id?: string
  appId?: string
  featureCode: string
  featureName: string
  featureDesc?: string
}

export interface FeatureTenant {
  tenantId: string
  tenantName?: string
  tenantCode?: string
  featureCodes: string[]
}

export interface FeatureMember {
  userId: string
  userName: string
  nickName?: string
  companyId?: string
  featureCodes: string[]
}

export interface TenantFeatureContext {
  appId: string
  tenantId: string
  userId: string
  platform: boolean
  tenantAdmin: boolean
  catalog: AppFeature[]
  tenantFeatures: AppFeature[]
  effectiveFeatureCodes: string[]
  tenants?: FeatureTenant[]
  members?: FeatureMember[]
}

export async function getTenantFeatureContext() {
  return unwrap(await get<TenantFeatureContext>('/tenant-features/context'))
}

export async function createAppFeature(payload: Pick<AppFeature, 'featureCode' | 'featureName' | 'featureDesc'>) {
  return unwrap(await post<AppFeature>('/tenant-features/catalog', payload))
}

export async function disableAppFeature(featureCode: string) {
  return unwrap(await del<void>(`/tenant-features/catalog/${encodeURIComponent(featureCode)}`))
}

export async function putTenantFeatures(tenantId: string, featureCodes: string[]) {
  return unwrap(await put<string[]>(`/tenant-features/tenants/${tenantId}`, { featureCodes }))
}

export async function putMemberFeatures(userId: string, featureCodes: string[]) {
  return unwrap(await put<string[]>(`/tenant-features/members/${userId}`, { featureCodes }))
}
