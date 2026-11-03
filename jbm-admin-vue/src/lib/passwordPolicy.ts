export function passwordPolicyError(password: string): string {
  if (password.length < 8) return '密码长度不能少于 8 位'
  if (password.length > 128) return '密码长度不能超过 128 位'
  const classes = [/[a-z]/, /[A-Z]/, /\d/, /[^A-Za-z0-9]/].filter((rule) => rule.test(password)).length
  return classes >= 3 ? '' : '密码必须包含大写字母、小写字母、数字、特殊字符中的至少 3 类'
}

export function generateInitialPassword(): string {
  if (!globalThis.crypto?.getRandomValues) throw new Error('当前浏览器不支持安全生成密码，请自行填写')
  const alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'
  const limit = Math.floor(256 / alphabet.length) * alphabet.length
  for (let attempt = 0; attempt < 100; attempt++) {
    let password = ''
    while (password.length < 10) {
      for (const value of crypto.getRandomValues(new Uint8Array(20))) {
        if (value < limit) password += alphabet[value % alphabet.length]
        if (password.length === 10) break
      }
    }
    if (!passwordPolicyError(password)) return password
  }
  throw new Error('密码生成失败，请重新生成')
}
