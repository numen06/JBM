import { get, post, unwrap } from './request'

/** Auth 图形验证码（Base64），对应 GET /captcha/vcode64 */
export async function fetchCaptchaBase64(width = 120, height = 40): Promise<string> {
  const res = await get<string>('/captcha/vcode64', {
    params: { width, height },
  })
  const raw = unwrap(res)
  if (raw.startsWith('data:')) return raw
  return `data:image/png;base64,${raw}`
}

/** 发送短信验证码（需先通过图形验证码），POST /captcha/pcode */
export async function sendSmsCode(phone: string, imageVcode: string): Promise<void> {
  const res = await post<boolean>('/captcha/pcode', null, {
    params: { phone, vcode: imageVcode },
  })
  unwrap(res)
}

export interface SmsCodeConfig {
  registrationRequired: boolean
  interval: number
  validTime: number
  debugBypass: boolean
}

export async function fetchSmsCodeConfig(): Promise<SmsCodeConfig> {
  const res = await get<SmsCodeConfig>('/captcha/pcode/config')
  return unwrap(res)
}
