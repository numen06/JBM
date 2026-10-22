import JSEncrypt from 'jsencrypt'

// Only public keys and signatures are sent to JBM Auth /oauth2/keys.
export type KeyRecord = { id: string; kind: string; name: string; created_at: number; last_used_at: number | null }
export type SshChallenge = { challengeId: string; message: string; namespace: string }
type FileChallenge = { challengeId: string; message: string; rpId: string }
type PasswordVerification = { expiresIn: number }
export type LoginKeyFile = {
  format: 'jbm-login-key'; version: 1; account: string; name: string;
  origin: string; rpId: string; keyId: string; publicKey: string; privateKey: string;
}

export function loginKeyFilename(key: LoginKeyFile) {
  return `${new URL(key.origin).host}-${key.account}-${key.name}-${key.keyId.slice(0, 8)}`.replace(/[^\p{L}\p{N}._-]/gu, '_') + '.jbmkey'
}

export function downloadLoginKey(key: LoginKeyFile, existingUrl?: string) {
  const blob = new Blob([JSON.stringify(key, null, 2)], { type: 'application/json' })
  const url = existingUrl || URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = loginKeyFilename(key)
  document.body.appendChild(link)
  link.click()
  link.remove()
  if (!existingUrl) setTimeout(() => URL.revokeObjectURL(url), 1000)
}

async function readLoginKey(file: File): Promise<LoginKeyFile> {
  if (!file.size || file.size > 16384) throw new Error('请选择有效的 JBM 密钥文件（最大 16 KB）')
  let key: LoginKeyFile
  try { key = JSON.parse(await file.text()) as LoginKeyFile } catch { throw new Error('密钥文件格式不正确') }
  if (!key || key.format !== 'jbm-login-key' || key.version !== 1
    || typeof key.account !== 'string' || typeof key.name !== 'string'
    || typeof key.keyId !== 'string' || !/^[a-f0-9]{64}$/.test(key.keyId)
    || typeof key.publicKey !== 'string' || key.publicKey.length > 4096 || !key.publicKey.startsWith('-----BEGIN PUBLIC KEY-----')
    || typeof key.privateKey !== 'string' || key.privateKey.length > 8192 || !key.privateKey.startsWith('-----BEGIN RSA PRIVATE KEY-----')
    || typeof key.rpId !== 'string' || key.rpId.length > 253) throw new Error('请选择由 JBM 下载的完整密钥文件')
  if (key.origin !== window.location.origin) throw new Error('此密钥文件属于其他平台地址，请在原地址登录')
  return key
}

function fileSignature(key: JSEncrypt, message: string): string {
  const signature = key.signSha256(message)
  if (!signature) throw new Error('密钥文件无效，无法完成签名')
  return signature
}

function decode(value: string): ArrayBuffer {
  const data = atob(value.replace(/-/g, '+').replace(/_/g, '/'))
  return Uint8Array.from(data, char => char.charCodeAt(0)).buffer
}
function encode(value: ArrayBuffer): string {
  return btoa(String.fromCharCode(...new Uint8Array(value))).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}
function credentialJSON(credential: PublicKeyCredential) {
  const response = credential.response
  const common = { id: credential.id, rawId: encode(credential.rawId), type: credential.type }
  if (response instanceof AuthenticatorAttestationResponse) {
    return { ...common, response: { clientDataJSON: encode(response.clientDataJSON),
      attestationObject: encode(response.attestationObject), transports: response.getTransports?.() || [] } }
  }
  const assertion = response as AuthenticatorAssertionResponse
  return { ...common, response: { clientDataJSON: encode(assertion.clientDataJSON),
    authenticatorData: encode(assertion.authenticatorData), signature: encode(assertion.signature),
    userHandle: assertion.userHandle ? encode(assertion.userHandle) : null } }
}
function requireBrowser() {
  if (!window.isSecureContext || !window.PublicKeyCredential) throw new Error('Passkey 需要 HTTPS 和支持通行密钥的浏览器')
}
export function keyLoginError(error: unknown): string {
  if (error instanceof DOMException && error.name === 'NotAllowedError') return '操作已取消或超时，请重试或选择其他登录方式'
  if (error instanceof DOMException && error.name === 'InvalidStateError') return '此设备已绑定通行密钥'
  return error instanceof Error ? error.message : '密钥操作失败'
}

export function createKeyClient(base: string, token: () => string) {
  async function authRequest<T>(path: string, body?: unknown, method = 'POST'): Promise<T> {
    const accessToken = token()
    const response = await fetch(`${base.replace(/\/$/, '')}${path}`, {
      method, headers: { 'Content-Type': 'application/json', ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}) },
      body: body === undefined ? undefined : JSON.stringify(body),
    })
    const result = await response.json()
    if (!response.ok || !result.success) throw new Error(result.message || '密钥操作失败')
    return result.result as T
  }
  const request = <T>(path: string, body?: unknown, method = 'POST') => authRequest<T>(`/oauth2/keys${path}`, body, method)
  return {
    list: () => request<KeyRecord[]>('', undefined, 'GET'),
    remove: (id: string) => request(`/${encodeURIComponent(id)}`, undefined, 'DELETE'),
    checkPasswordVerification: () => request<PasswordVerification>('/verify-password', {}),
    async verifyPassword(password: string): Promise<PasswordVerification> {
      if (!password) throw new Error('请输入当前账号密码')
      const identity = await authRequest<{ clientId: string }>('/oauth2/userinfo', undefined, 'GET')
      const publicKeyBase64 = await authRequest<string>(`/oauth2/publicKey?app_id=${encodeURIComponent(identity.clientId)}`, undefined, 'GET')
      const encrypt = new JSEncrypt()
      encrypt.setPublicKey(`-----BEGIN PUBLIC KEY-----\n${publicKeyBase64}\n-----END PUBLIC KEY-----`)
      const encrypted = encrypt.encrypt(password)
      if (!encrypted) throw new Error('密码加密失败，请重试')
      return request<PasswordVerification>('/verify-password', { password: encrypted })
    },
    async registerFileKey(name: string): Promise<LoginKeyFile> {
      if (!window.crypto?.getRandomValues) throw new Error('当前浏览器不支持安全生成密钥')
      // Paint the busy state first; JSEncrypt's timer-based generation is heavily
      // throttled in background tabs. Generate locally without system credentials.
      await new Promise<void>(resolve => setTimeout(resolve, 0))
      let key: JSEncrypt
      do {
        key = new JSEncrypt({ default_key_size: '2048' })
        // JSEncrypt can produce a 2047-bit product from two 1024-bit primes.
        // Keep Auth's 2048-bit minimum rather than relaxing public-key validation.
      } while ((key.getKey() as unknown as { n: { bitLength(): number } }).n.bitLength() !== 2048)
      const publicKey = key.getPublicKey()
      const data = await request<FileChallenge & { account: string; keyId: string }>('/register-options', { kind: 'FILE_KEY', name, publicKey })
      await request('/register', { kind: 'FILE_KEY', challengeId: data.challengeId, signature: fileSignature(key, data.message) })
      return { format: 'jbm-login-key', version: 1, account: data.account, name,
        origin: window.location.origin, rpId: data.rpId, keyId: data.keyId, publicKey, privateKey: key.getPrivateKey() }
    },
    async fileKeyProof(file: File, clientId: string): Promise<string> {
      const keyFile = await readLoginKey(file)
      const key = new JSEncrypt()
      try {
        key.setPrivateKey(keyFile.privateKey)
        if (key.getPublicKey().replace(/\s/g, '') !== keyFile.publicKey.replace(/\s/g, '')) throw new Error('mismatch')
      } catch { throw new Error('密钥文件已损坏，公私钥不匹配') }
      const data = await request<FileChallenge>('/login-options', { kind: 'FILE_KEY', client_id: clientId })
      if (keyFile.rpId !== data.rpId) throw new Error('此密钥文件不属于当前平台')
      return JSON.stringify({ challengeId: data.challengeId, publicKey: keyFile.publicKey, signature: fileSignature(key, data.message) })
    },
    async registerPasskey(name: string) {
      requireBrowser()
      const data = await request<{ challengeId: string; publicKey: Omit<PublicKeyCredentialCreationOptions, 'challenge' | 'user' | 'excludeCredentials'> & { challenge: string; user: Omit<PublicKeyCredentialUserEntity, 'id'> & { id: string }; excludeCredentials: { id: string; type: 'public-key' }[] } }>('/register-options', { name })
      const options = data.publicKey
      const credential = await navigator.credentials.create({ publicKey: { ...options,
        challenge: decode(options.challenge), user: { ...options.user, id: decode(options.user.id) },
        excludeCredentials: options.excludeCredentials?.map(item => ({ ...item, id: decode(item.id) })),
      } }) as PublicKeyCredential | null
      if (!credential) throw new Error('未创建通行密钥')
      await request('/register', { challengeId: data.challengeId, credential: credentialJSON(credential) })
    },
    async passkeyProof(clientId: string): Promise<string> {
      requireBrowser()
      const data = await request<{ challengeId: string; publicKey: { challenge: string; rpId: string; timeout: number; userVerification: UserVerificationRequirement } }>('/login-options', { client_id: clientId })
      const credential = await navigator.credentials.get({ publicKey: { ...data.publicKey, challenge: decode(data.publicKey.challenge) } }) as PublicKeyCredential | null
      if (!credential) throw new Error('未选择通行密钥')
      return JSON.stringify({ challengeId: data.challengeId, credential: credentialJSON(credential) })
    },
    sshOptions: (clientId: string) => request<SshChallenge>('/login-options', { kind: 'SSH_KEY', client_id: clientId }),
    sshRegisterOptions: (name: string, publicKey: string) => request<SshChallenge>('/register-options', { kind: 'SSH_KEY', name, publicKey }),
    sshRegister: (challengeId: string, signature: string) => request('/register', { kind: 'SSH_KEY', challengeId, signature }),
  }
}

export function downloadChallenge(challenge: SshChallenge) {
  const url = URL.createObjectURL(new Blob([challenge.message], { type: 'text/plain;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = 'jbm-login-challenge.txt'
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
