/** 与后端 com.jbm.cluster.api.constants.LoginType 对齐 */
export type OAuthLoginType = 'PASSWORD' | 'SMS' | 'FACE' | 'WECHAT' | 'MINIAPP'

export type LoginTabId = OAuthLoginType | 'SCAN' | 'AUTH_CODE' | 'THIRD_PARTY'

/** sessionStorage：授权码模式跳转前写入，回调页校验 CSRF */
export const OAUTH2_STATE_STORAGE_KEY = 'jbm_oauth2_state'
export const OAUTH2_REDIRECT_STORAGE_KEY = 'jbm_oauth2_redirect_uri'

export interface LoginTabMeta {
  id: LoginTabId
  label: string
  description: string
}

export const LOGIN_TABS: LoginTabMeta[] = [
  { id: 'PASSWORD', label: '密码', description: '用户名 + 密码 + 图形验证码（OAuth2 密码模式 grant_type=password）' },
  {
    id: 'AUTH_CODE',
    label: '授权码',
    description: 'OAuth2 授权码模式：/oauth2/authorize → code → POST /oauth2/token（authorization_code）',
  },
  { id: 'SMS', label: '短信', description: '手机号 + 短信验证码（loginType=SMS）' },
  { id: 'SCAN', label: '扫码', description: '已登录移动端扫码确认（/qrcode）' },
  { id: 'FACE', label: '人脸', description: '手机号 + 人脸照片 Base64（loginType=FACE，需 Center 百度人脸）' },
  { id: 'WECHAT', label: '微信', description: 'OpenID + 微信授权 code（loginType=WECHAT）' },
  { id: 'MINIAPP', label: '小程序', description: '手机号 + 小程序登录 code（loginType=MINIAPP）' },
  { id: 'THIRD_PARTY', label: '第三方', description: '第三方 IdP 回调 code → /oauth2/thirdparty/{provider}/callback' },
]

/** 开发环境图形/短信验证码直通（与后端 VCoder/PCoder 一致） */
export const DEV_CAPTCHA_CODE = '9999'
export const DEV_SMS_CODE = '99999'

/** 种子应用（Auth 重启或 POST /internal/dev/reset-jaja7-seed 后与库一致） */
export const JBM_SEED_CLIENT_ID = 'jbmSeedDevAppKey00000001'
export const JBM_SEED_CLIENT_SECRET = 'jbmSeedDevSecret0000000001'
export const JBM_SEED_PASSWORD = 'admin'

/** jaja7 登录页默认：优先 .env；未配置时 demo 与当前库一致 */
export const JBM_DEFAULT_USERNAME = 'admin'
export const JBM_DEFAULT_PASSWORD =
  import.meta.env.VITE_LOGIN_PASSWORD?.trim() || (import.meta.env.DEV ? 'Admin@123' : JBM_SEED_PASSWORD)
export const JBM_DEFAULT_CLIENT_ID =
  import.meta.env.VITE_OAUTH_CLIENT_ID?.trim() || (import.meta.env.DEV ? 'demo' : JBM_SEED_CLIENT_ID)
export const JBM_DEFAULT_CLIENT_SECRET =
  import.meta.env.VITE_OAUTH_CLIENT_SECRET?.trim() || (import.meta.env.DEV ? 'demo123' : JBM_SEED_CLIENT_SECRET)
export const JBM_DEFAULT_OAUTH_SCOPE = 'all'
