export function passwordPolicyError(password: string): string {
  if (password.length < 8) return '密码长度不能少于 8 位'
  if (password.length > 128) return '密码长度不能超过 128 位'
  const classes = [/[a-z]/, /[A-Z]/, /\d/, /[^A-Za-z0-9]/].filter((rule) => rule.test(password)).length
  return classes >= 3 ? '' : '密码必须包含大写字母、小写字母、数字、特殊字符中的至少 3 类'
}
