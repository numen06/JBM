import http, { get, post, put, del, unwrap } from './request'

export type BackupPolicy = { enabled: boolean; frequency: 'daily' | 'weekly'; time: string; weekday: number; retentionDays: number; nextRunAt?: string | null }
export type DatabaseSource = { id: string; name: string; host: string; port: number; database: string; databaseType: string; username: string; sslMode: 'PREFERRED' | 'REQUIRED'; isDefault: boolean; status: string; version?: string; tableCount?: number; error?: string; checkedAt?: string; lastBackupAt?: string; policy: BackupPolicy }
export type DatabaseTask = { id: string; kind: 'backup' | 'restore'; sourceId: string; sourceName: string; database: string; status: string; origin?: string; createdAt: string; startedAt?: string; finishedAt?: string; error?: string; size?: number; filename?: string; sha256?: string; targetDatabase?: string; mode?: string; verifiedTables?: number; safetyBackupId?: string }
const base = '/job/sysJob/database-backups'

export const listDatabaseSources = async () => unwrap(await get<DatabaseSource[]>(`${base}/sources`))
export const saveDatabaseSource = async (body: Record<string, unknown>) => unwrap(await post<DatabaseSource>(`${base}/sources`, body))
export const testDatabaseSource = async (id: string) => unwrap(await post<DatabaseSource>(`${base}/sources/${id}/test`, {}))
export const discoverDatabases = async (id: string) => unwrap(await get<Array<{ database: string; tableCount: number; connected: boolean }>>(`${base}/sources/${id}/databases`))
export const attachDatabase = async (id: string, database: string) => unwrap(await post<DatabaseSource>(`${base}/sources/${id}/databases`, { database }))
export const saveBackupPolicy = async (id: string, policy: BackupPolicy) => unwrap(await put<BackupPolicy>(`${base}/sources/${id}/policy`, policy))
export const startDatabaseBackup = async (sourceId: string) => unwrap(await post<DatabaseTask>(`${base}/backups`, { sourceId }))
export const listDatabaseTasks = async (kind: string, page: number, sourceId?: string) => unwrap(await get<{ items: DatabaseTask[]; total: number }>(`${base}/tasks`, { params: { kind, page, size: 20, sourceId: sourceId || undefined } }))
export const startDatabaseRestore = async (body: Record<string, unknown>) => unwrap(await post<DatabaseTask>(`${base}/restores`, body))
export const deleteDatabaseBackup = async (id: string) => unwrap(await del<boolean>(`${base}/backups/${id}`))
export async function downloadDatabaseBackup(task: DatabaseTask) {
  const { data } = await http.get<Blob>(`${base}/backups/${task.id}/file`, { responseType: 'blob' })
  const url = URL.createObjectURL(data)
  const link = document.createElement('a')
  link.href = url
  link.download = task.filename || `${task.id}.sql.gz`
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}
