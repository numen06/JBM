import { get, put, unwrap } from './request'
import type { DataPaging } from './types'

export interface OperatorApplication {
  id?: string | number
  appId?: string | number
  tenantId?: string | number
  applicantUserId?: string | number
  status?: number
  reason?: string
  reviewRemark?: string
  reviewedBy?: string | number
  reviewedAt?: string
  createTime?: string
}

export async function listOperatorApplications(status?: number) {
  const res = await get<DataPaging<OperatorApplication>>('/operator-application', {
    params: {
      'pageForm.currPage': 1,
      'pageForm.pageSize': 100,
      ...(status == null ? {} : { status }),
    },
  })
  return unwrap(res)
}

export async function reviewOperatorApplication(
  id: string | number,
  status: 1 | 2,
  reviewRemark = '',
) {
  const res = await put<OperatorApplication>(`/operator-application/${id}/review`, {
    status,
    reviewRemark,
  })
  return unwrap(res)
}
