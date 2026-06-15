import { get, post, unwrap } from './request'
import type {
  DataPaging,
  WebhookEventConfig,
  WebhookEventConfigQuery,
  WebhookTask,
  WebhookTaskQuery,
} from './types'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'

const CONFIG_BASE = '/push/webhookEventConfig'
const TASK_BASE = '/push/webhookTask'

function pageForm(page: number, size: number, sortRule = 'updateTime:desc') {
  return { currPage: page, pageSize: size, sortRule }
}

function configFilter(query: WebhookEventConfigQuery = {}) {
  const keyword = query.keyword?.trim()
  return {
    ...(keyword ? { keyword } : {}),
    webhookEventConfig: {
      ...(query.businessEventCode?.trim() ? { businessEventCode: query.businessEventCode.trim() } : {}),
      ...(query.eventName?.trim() ? { eventName: query.eventName.trim() } : {}),
      ...(query.eventGroup?.trim() ? { eventGroup: query.eventGroup.trim() } : {}),
      ...(query.serviceName?.trim() ? { serviceName: query.serviceName.trim() } : {}),
      ...(query.enable === '' || query.enable == null ? {} : { enable: query.enable }),
    },
  }
}

function taskFilter(query: WebhookTaskQuery = {}) {
  return {
    ...(query.beginTime ? { beginTime: query.beginTime } : {}),
    ...(query.endTime ? { endTime: query.endTime } : {}),
    webhookTask: {
      ...(query.httpStatus === '' || query.httpStatus == null ? {} : { httpStatus: query.httpStatus }),
      ...(query.status ? { status: query.status } : {}),
    },
    webhookEventConfig: {
      ...(query.businessEventCode?.trim() ? { businessEventCode: query.businessEventCode.trim() } : {}),
      ...(query.eventName?.trim() ? { eventName: query.eventName.trim() } : {}),
      ...(query.eventGroup?.trim() ? { eventGroup: query.eventGroup.trim() } : {}),
    },
  }
}

export async function listWebhookEventConfigs(
  page = 1,
  size = DEFAULT_PAGE_SIZE,
  query: WebhookEventConfigQuery = {},
) {
  const res = await post<DataPaging<WebhookEventConfig>>(`${CONFIG_BASE}/selectWebhookEventConfigs`, {
    ...configFilter(query),
    pageForm: pageForm(page, size),
  })
  return unwrap(res)
}

export async function saveWebhookEventConfig(config: Partial<WebhookEventConfig>) {
  const res = await post<WebhookEventConfig>(`${CONFIG_BASE}/saveConfig`, config)
  return unwrap(res)
}

export async function findWebhookEventConfig(query: Partial<WebhookEventConfig>) {
  const res = await post<WebhookEventConfig | null>(`${CONFIG_BASE}/findConfig`, query)
  return unwrap(res)
}

export async function listWebhookTasks(page = 1, size = DEFAULT_PAGE_SIZE, query: WebhookTaskQuery = {}) {
  const res = await post<DataPaging<WebhookTask>>(`${TASK_BASE}/selectWebhookTasks`, {
    ...taskFilter(query),
    pageForm: pageForm(page, size, 'createTime:desc'),
  })
  return unwrap(res)
}

export async function findWebhookTask(query: Partial<WebhookTask>) {
  const res = await post<WebhookTask | null>(`${TASK_BASE}/findTask`, query)
  return unwrap(res)
}

export async function retryWebhookTask(taskId: string) {
  const res = await get<WebhookTask>(`${TASK_BASE}/retry`, { params: { taskId } })
  return unwrap(res)
}

export async function triggerWebhookEvent(eventId: string) {
  const res = await get<{ sent?: number; tasks?: WebhookTask[] }>(`${TASK_BASE}/run`, { params: { eventId } })
  return unwrap(res)
}

export async function sendWebhookTask(task: Partial<WebhookTask>) {
  const res = await post<WebhookTask>(`${TASK_BASE}/req`, task)
  return unwrap(res)
}
