import { post, unwrap } from './request'
import type { BaseAccountLog, DataPaging } from './types'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'

export async function listAccountLogs(page = 1, size = DEFAULT_PAGE_SIZE) {
  const res = await post<DataPaging<BaseAccountLog>>('/baseAccountLogs/pageList', {
    pageForm: { currPage: page, pageSize: size },
  })
  return unwrap(res)
}
