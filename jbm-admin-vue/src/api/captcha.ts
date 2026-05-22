import { get, unwrap } from './request'

/** Auth 图形验证码（Base64），对应 GET /captcha/vcode64 */
export async function fetchCaptchaBase64(width = 120, height = 40): Promise<string> {
  const res = await get<string>('/captcha/vcode64', {
    params: { width, height },
  })
  const raw = unwrap(res)
  if (raw.startsWith('data:')) return raw
  return `data:image/png;base64,${raw}`
}
