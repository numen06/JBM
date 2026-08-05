# @jbm7/sdk

JBM 7.3 基础技术平台的无框架浏览器 SDK。宿主负责提供 Token、租户和未授权处理；SDK 不依赖 Vue、Router、Pinia 或浏览器存储。

```ts
import { createJbmClient, createJbmServiceClient } from '@jbm7/sdk'

const client = createJbmClient({ baseUrl: '/v3/api/', tokenProvider })
const center = createJbmServiceClient(client, 'center')
const user = await center.get('current/user')
```

浏览器 OAuth 客户端使用授权码 + PKCE，不在前端配置 `client_secret`。
