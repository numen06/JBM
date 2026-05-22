import JSEncrypt from 'jsencrypt'
import { get, unwrap } from '@/api/request'

/** Hutool RSA publicKeyBase64 -> PEM */
export function toPemPublicKey(base64Key: string): string {
  const body = base64Key.replace(/\s/g, '')
  const lines = body.match(/.{1,64}/g) ?? [body]
  return `-----BEGIN PUBLIC KEY-----\n${lines.join('\n')}\n-----END PUBLIC KEY-----`
}

export async function fetchPublicKey(appId: string): Promise<string> {
  const res = await get<string>('/oauth2/publicKey', { params: { app_id: appId } })
  return unwrap(res)
}

export function encryptPassword(plainPassword: string, publicKeyBase64: string): string {
  const encrypt = new JSEncrypt()
  encrypt.setPublicKey(toPemPublicKey(publicKeyBase64))
  const encrypted = encrypt.encrypt(plainPassword)
  if (!encrypted) {
    throw new Error('密码加密失败，请检查公钥配置')
  }
  return encrypted
}

export async function encryptPasswordForClient(
  plainPassword: string,
  clientId: string,
): Promise<string> {
  const publicKey = await fetchPublicKey(clientId)
  return encryptPassword(plainPassword, publicKey)
}
