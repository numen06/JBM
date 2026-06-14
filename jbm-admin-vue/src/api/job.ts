import { del, get, post, put, unwrap } from './request'
import type { DataPaging, SysJob, SysJobLog } from './types'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'

const JOB_BASE = '/job/sysJob'
const LOG_BASE = '/job/sysJobLog'

export interface JobListQuery {
  keyword?: string
  jobGroup?: string
  status?: SysJob['status'] | ''
}

export interface JobLogListQuery {
  keyword?: string
  jobGroup?: string
  status?: string
}

function pageForm(page: number, size: number, sortRule = 'createTime:desc') {
  return { currPage: page, pageSize: size, sortRule }
}

function normalizeStatus(status?: SysJob['status'] | '') {
  if (status === '' || status == null) return undefined
  if (status === 0) return 'NORMAL'
  if (status === 1) return 'PAUSE'
  return status
}

function jobFilter(query: JobListQuery = {}) {
  const keyword = query.keyword?.trim()
  return {
    ...(keyword ? { jobName: keyword } : {}),
    ...(query.jobGroup?.trim() ? { jobGroup: query.jobGroup.trim() } : {}),
    ...(normalizeStatus(query.status) ? { status: normalizeStatus(query.status) } : {}),
  }
}

export async function listJobs(page = 1, size = DEFAULT_PAGE_SIZE, query: JobListQuery = {}) {
  const res = await post<DataPaging<SysJob>>(`${JOB_BASE}/pageList`, {
    sysJob: jobFilter(query),
    pageForm: pageForm(page, size),
  })
  return unwrap(res)
}

export async function getJob(jobId: number) {
  const res = await get<SysJob>(`${JOB_BASE}/${jobId}`)
  return unwrap(res)
}

export async function createJob(data: Partial<SysJob>) {
  const res = await post<number>(JOB_BASE, data)
  return unwrap(res)
}

export async function updateJob(jobId: number, data: Partial<SysJob>) {
  const res = await put<boolean>(`${JOB_BASE}/${jobId}`, data)
  return unwrap(res)
}

export async function deleteJob(jobId: number) {
  const res = await del<number>(`${JOB_BASE}/${jobId}`)
  return unwrap(res)
}

export async function changeJobStatus(jobId: number, status: SysJob['status']) {
  const res = await put<SysJob>(`${JOB_BASE}/changeStatus`, { jobId, status: normalizeStatus(status) })
  return unwrap(res)
}

export async function runJob(jobId: number) {
  const res = await put<SysJob>(`${JOB_BASE}/run`, { jobId })
  return unwrap(res)
}

export async function listJobLogs(page = 1, size = DEFAULT_PAGE_SIZE, query: JobLogListQuery = {}) {
  const keyword = query.keyword?.trim()
  const res = await post<DataPaging<SysJobLog>>(`${LOG_BASE}/pageList`, {
    sysJobLog: {
      ...(keyword ? { jobName: keyword } : {}),
      ...(query.jobGroup?.trim() ? { jobGroup: query.jobGroup.trim() } : {}),
      ...(query.status ? { status: query.status } : {}),
    },
    pageForm: pageForm(page, size, 'startTime:desc'),
  })
  return unwrap(res)
}

export async function cleanJobLogs() {
  const res = await del<void>(`${LOG_BASE}/clean`)
  return unwrap(res)
}
