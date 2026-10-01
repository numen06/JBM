// Wire format shared with JBM Auth /oauth2/keys. Never accepts private keys.
export type KeyRecord = { id: string; kind: string; name: string; created_at: number; last_used_at: number | null }
export type SshChallenge = { challengeId: string; message: string; namespace: string }

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
  async function request<T>(path: string, body?: unknown, method = 'POST'): Promise<T> {
    const accessToken = token()
    const response = await fetch(`${base.replace(/\/$/, '')}/oauth2/keys${path}`, {
      method, headers: { 'Content-Type': 'application/json', ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}) },
      body: body === undefined ? undefined : JSON.stringify(body),
    })
    const result = await response.json()
    if (!response.ok || !result.success) throw new Error(result.message || '密钥操作失败')
    return result.result as T
  }
  return {
    list: () => request<KeyRecord[]>('', undefined, 'GET'),
    remove: (id: string) => request(`/${encodeURIComponent(id)}`, undefined, 'DELETE'),
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
