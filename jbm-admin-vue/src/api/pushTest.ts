import { get, post, unwrap } from './request'
import type { PushTestAck, PushTestRequest, PushTestTaskStatus } from './types'

const BASE = '/push/pushTest'

export async function sendPushTest(request: PushTestRequest) {
  const res = await post<PushTestTaskStatus>(`${BASE}/send`, request)
  return unwrap(res)
}

export async function startPushPerfTest(request: PushTestRequest) {
  const res = await post<PushTestTaskStatus>(`${BASE}/perf`, request)
  return unwrap(res)
}

export async function getPushPerfStatus(taskId: string) {
  const res = await get<PushTestTaskStatus>(`${BASE}/perf/${taskId}`)
  return unwrap(res)
}

export async function ackPushTestMessage(ack: PushTestAck) {
  const res = await post<PushTestTaskStatus>(`${BASE}/ack`, ack)
  return unwrap(res)
}
