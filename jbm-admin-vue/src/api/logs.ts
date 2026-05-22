import { post, unwrap } from './request'
import type { BaseAccountLog, DataPaging } from './types'

export async function listAccountLogs(page = 1, size = 20) {
  const res = await post<DataPaging<BaseAccountLog>>('/baseAccountLogs/pageList', {
    pageForm: { currPage: page, pageSize: size },
  })
  return unwrap(res)
}
