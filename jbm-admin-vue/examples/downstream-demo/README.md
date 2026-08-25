# JBM 下游 npm 集成 Demo

本示例只通过 npm 包接入 JBM 管理平台，并额外注册一个下游页面，不引用仓库源码。

```powershell
$plain = '<用户名>:<密码>'
$env:JBM_NPM_AUTH = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($plain))
npm install
npm run dev
```

访问 <http://127.0.0.1:5180/login>，登录后打开“下游示例 / 集成验证”。默认代理到本机 `6060` Gateway；可通过 `JBM_GATEWAY_URL` 修改。
