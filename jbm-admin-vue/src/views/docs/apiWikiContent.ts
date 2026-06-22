export interface DocSection {
  id: string
  title: string
  group: string
}

export interface ApiEndpoint {
  method: 'GET' | 'POST' | 'PUT' | 'DELETE'
  path: string
  desc: string
  params?: { name: string; type: string; required?: boolean; desc: string }[]
  request?: string
  response?: string
}

export const docSections: DocSection[] = [
  { id: 'quick-start', title: '快速开始', group: '入门' },
  { id: 'platform-capability', title: '平台能力地图', group: '入门' },
  { id: 'register-account', title: '1. 注册 JBM 账号', group: '入门' },
  { id: 'create-app', title: '2. 创建子应用', group: '入门' },
  { id: 'choose-mode', title: '3. 选择接入方式', group: '入门' },
  { id: 'oauth2-auth-code', title: '授权码模式', group: 'OAuth2' },
  { id: 'oauth2-login-code', title: '登录授权码', group: 'OAuth2' },
  { id: 'oauth2-refresh', title: '刷新 Token', group: 'OAuth2' },
  { id: 'openapi-api-key', title: 'API Key 与签名', group: 'OpenAPI' },
  { id: 'openapi-isolation', title: '租户与数据隔离', group: 'OpenAPI' },
  { id: 'api-auth', title: '认证接口 /oauth2/*', group: 'API 参考' },
  { id: 'api-user', title: '用户接口 /user/*', group: 'API 参考' },
  { id: 'api-authority', title: '权限接口 /authority/*', group: 'API 参考' },
  { id: 'api-developer', title: '开发者接口 /developer/*', group: 'API 参考' },
  { id: 'sdk-frontend', title: '前端接入示例', group: 'SDK 与示例' },
  { id: 'sdk-backend', title: '后端接入示例', group: 'SDK 与示例' },
  { id: 'published-api', title: '已发布开放 API', group: 'API 参考' },
  { id: 'faq', title: '常见问题', group: 'FAQ' },
]

export const gatewayBase = 'http://127.0.0.1:6060'

export const authEndpoints: ApiEndpoint[] = [
  {
    method: 'POST',
    path: '/oauth2/register',
    desc: '用户自助注册。前端先获取 RSA 公钥并加密密码，再带 X-Password-Encrypted 提交。',
    params: [
      { name: 'userName', type: 'string', required: true, desc: '登录用户名' },
      { name: 'password', type: 'string', required: true, desc: 'RSA 加密后的密码' },
      { name: 'vcode', type: 'string', required: true, desc: '图形验证码' },
      { name: 'client_id', type: 'string', required: true, desc: 'OAuth2 客户端 ID' },
      { name: 'client_secret', type: 'string', required: true, desc: 'OAuth2 客户端密钥' },
      { name: 'nickName', type: 'string', desc: '昵称，可选' },
      { name: 'email', type: 'string', desc: '邮箱，可选' },
      { name: 'mobile', type: 'string', desc: '手机号，可选' },
    ],
    request: `POST ${gatewayBase}/oauth2/register
Content-Type: application/x-www-form-urlencoded
X-Password-Encrypted: true

userName=developer&password=<RSA_ENCRYPTED>&vcode=9999&client_id=demo`,
    response: `{ "success": true, "code": 200, "message": "操作成功" }`,
  },
  {
    method: 'POST',
    path: '/oauth2/token',
    desc: '获取 Access Token，用户登录使用 authorization_code，服务端调用使用 client_credentials。',
    params: [
      { name: 'grant_type', type: 'string', required: true, desc: 'authorization_code | refresh_token | client_credentials' },
      { name: 'client_id', type: 'string', required: true, desc: '应用 Client ID' },
      { name: 'client_secret', type: 'string', desc: '服务端应用或 client_credentials 使用，浏览器不传' },
      { name: 'code', type: 'string', desc: '授权码模式 code' },
      { name: 'redirect_uri', type: 'string', desc: '授权码模式回调地址' },
    ],
    request: `POST ${gatewayBase}/oauth2/token
Content-Type: application/x-www-form-urlencoded
X-Password-Encrypted: true

grant_type=authorization_code&client_id=demo&code=AUTH_CODE&redirect_uri=https://app.example.com/login/callback`,
    response: `{
  "access_token": "eyJ...",
  "refresh_token": "eyJ...",
  "expires_in": 7200,
  "token_type": "Bearer",
  "scope": "all"
}`,
  },
  {
    method: 'GET',
    path: '/oauth2/authorize',
    desc: 'OAuth2 授权页，适用于第三方 Web 或移动端登录跳转。',
    params: [
      { name: 'response_type', type: 'string', required: true, desc: '固定 code' },
      { name: 'client_id', type: 'string', required: true, desc: 'Client ID' },
      { name: 'redirect_uri', type: 'string', required: true, desc: '已登记回调地址' },
      { name: 'scope', type: 'string', desc: '权限范围，例如 all' },
      { name: 'state', type: 'string', desc: 'CSRF 防重放随机串' },
    ],
  },
  {
    method: 'POST',
    path: '/oauth2/refresh',
    desc: '使用 refresh_token 刷新 Access Token。',
    params: [
      { name: 'grant_type', type: 'string', required: true, desc: 'refresh_token' },
      { name: 'refresh_token', type: 'string', required: true, desc: '刷新令牌' },
      { name: 'client_id', type: 'string', required: true, desc: 'Client ID' },
      { name: 'client_secret', type: 'string', required: true, desc: 'Client Secret' },
    ],
  },
  {
    method: 'GET',
    path: '/oauth2/userinfo',
    desc: '获取当前登录用户信息。',
    params: [{ name: 'Authorization', type: 'header', required: true, desc: 'Bearer {access_token}' }],
  },
  {
    method: 'DELETE',
    path: '/oauth2/logout',
    desc: '登出并销毁 Token。',
  },
  {
    method: 'GET',
    path: '/oauth2/publicKey',
    desc: '获取 RSA 公钥，供密码加密使用。',
    params: [{ name: 'client_id', type: 'string', required: true, desc: 'Client ID' }],
  },
]

export const openApiHeaders = [
  { name: 'X-JBM-Api-Key', desc: '开发者或应用 API Key 的 key 值' },
  { name: 'X-JBM-Timestamp', desc: '毫秒时间戳，服务端按时间窗口防重放' },
  { name: 'X-JBM-Nonce', desc: '一次性随机串，建议 UUID' },
  { name: 'X-JBM-Signature', desc: '使用 secret 对 method、path、query、bodyHash、timestamp、nonce 做 HMAC-SHA256' },
]
