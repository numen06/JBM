/**
 * 雪花 ID / Java Long 类型安全工具。
 *
 * 后端主键多为 Long（雪花算法），JSON 中常以字符串返回（如 `"2066732840298020866"`）。
 * JavaScript `Number` 仅能安全表示 ±(2^53 - 1)（9007199254740991），更大 ID 经
 * `Number(id)`、`parseInt(id)`、`+id` 会丢失精度，导致树组装失败、父子匹配错误、
 * API 查不到记录等问题。
 *
 * @example
 * // ❌ 禁止对实体主键使用
 * const id = Number(org.parentId)
 * map.set(Number(row.id), row)
 *
 * // ✅ 比较、Map 键、树节点关联
 * sameSnowflakeId(a, b)
 * map.set(toSnowflakeIdString(row.id), row)
 *
 * // ✅ 查询参数 / 请求体
 * params.companyId = toSnowflakeIdParam(form.companyId)
 */
export const MAX_SAFE_JS_INTEGER = 9007199254740991

/** 后端 Long / 雪花 ID；消费侧请用本模块工具，禁止直接 Number() */
export type SnowflakeId = string | number

export function isSnowflakeId(value: unknown): value is SnowflakeId {
  return typeof value === 'string' || typeof value === 'number'
}

/** 是否为空 ID（null / undefined / '' / 0 / '0'） */
export function isBlankSnowflakeId(
  value: unknown,
): value is null | undefined | '' | 0 | '0' {
  return value == null || value === '' || value === 0 || value === '0'
}

/** 规范为字符串，供比较、Map 键、树 parentId 关联等使用 */
export function toSnowflakeIdString(value: SnowflakeId): string {
  return String(value).trim()
}

/** 两 ID 是否相同（字符串比较，避免 Number 精度问题） */
export function sameSnowflakeId(a?: SnowflakeId | null, b?: SnowflakeId | null): boolean {
  if (a == null || b == null) return false
  return toSnowflakeIdString(a) === toSnowflakeIdString(b)
}

/** 是否在 JS 安全整数范围内（仅作判断；实体主键仍建议保持字符串） */
export function isSafeJsIntegerId(value: SnowflakeId): boolean {
  const s = toSnowflakeIdString(value)
  if (!/^\d+$/.test(s)) return false
  if (s.length > 16) return false
  if (s.length === 16 && s > String(MAX_SAFE_JS_INTEGER)) return false
  const n = Number(s)
  return Number.isSafeInteger(n) && String(n) === s
}

/**
 * 写入 API 查询参数或请求体的 ID。
 * 超出安全整数时保持字符串；小 ID 仍可发 number 以兼容历史接口。
 */
export function toSnowflakeIdParam(value: SnowflakeId): string | number {
  if (isSafeJsIntegerId(value)) return Number(value)
  return toSnowflakeIdString(value)
}

/** 可选 ID：空值返回 undefined，否则走 {@link toSnowflakeIdParam} */
export function optionalSnowflakeIdParam(
  value?: SnowflakeId | null,
): string | number | undefined {
  if (isBlankSnowflakeId(value)) return undefined
  return toSnowflakeIdParam(value!)
}

/** 从路由 query 等解析 ID，无效则返回 undefined */
export function parseSnowflakeIdFromQuery(value: unknown): string | undefined {
  if (value == null || value === '') return undefined
  const raw = Array.isArray(value) ? value[0] : value
  if (raw == null || raw === '') return undefined
  return toSnowflakeIdString(raw as SnowflakeId)
}
