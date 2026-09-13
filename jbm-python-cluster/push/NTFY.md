# ntfy 通知渠道

管理页面：消息管理 → 渠道配置 → 新增配置 → ntfy 推送。

填写服务地址、默认主题、访问 Token、默认优先级，并启用配置。服务地址示例为
`https://notify.hz-aitech.com`，主题示例为 `dangxiao-alerts`。Token 使用该主题的发布凭据，
不要把管理员密码放入业务消息。保存后可以点击“测试发送”。

配置持久化到现有 Push 渠道配置表，类型为 `8`。配置列表和保存响应中的 Token 显示为
`********`；编辑时保持该值会保留原 Token，清空会移除认证。

旧 MySQL 消息表必须有 `extend_data` 或 `extend` 字段才能保存消息 ID 和失败原因。
若两者均不存在，执行 `migrations/20260913_ntfy_delivery_details.sql` 并重启 Push。
该迁移仅增加可空字段，回退应用版本时保留字段即可。

## 发送接口

通过现有平台认证调用 `POST /push/notification/send/ntfy`（网关基础路径按部署环境添加）。
Push 服务直连路径为 `/notification/send/ntfy`。

```json
{
  "ntfyConfigId": 123,
  "title": "路灯运行通知",
  "content": "路灯已开启",
  "priority": 3,
  "showInMessageCenter": true
}
```

`ntfyConfigId` 可省略，此时选择第一个已启用的 ntfy 配置。`topic`、`priority` 可按消息覆盖；
服务地址和 Token 只取自渠道配置。发布账号必须具有目标主题的写权限。

沿用现有 RabbitMQ 投递流程；需要立即检查 ntfy 回执时可传 `syncDelivery: true`。
消息中心记录渠道、成功或失败状态、ntfy 消息 ID 与主题。
`sent` 表示 ntfy 已接收，不代表手机已展示或用户已读；认证失败、超时和无效回执不会记为成功。

## 手机订阅与 WebSocket

手机订阅与后端发布使用不同连接：JBM Push 通过 HTTPS 发布，订阅端可通过 WSS 接收。
手机中填写相同服务地址和主题，使用具有主题读取权限的账号；发布专用 Token 无法订阅。
Android 客户端可在连接协议设置中选择 WebSocket。服务端与反向代理需支持 Upgrade。

订阅地址示例为 `wss://notify.hz-aitech.com/dangxiao-alerts/ws`。
应用切换协议后，配置中的 HTTPS 服务地址无需更改。

官方接口说明：https://docs.ntfy.sh/subscribe/api/#websockets

## 验证

在 `jbm-python-cluster` 运行：

```powershell
.venv/Scripts/python.exe -m pytest push/tests/test_ntfy.py push/tests/test_push_compat.py -q
```

在 `jbm-admin-vue` 运行 `npm run build:app`。

2026-09-13 验证：31 项后端测试通过，管理端构建通过。本地 JBM Push 使用自建服务器的
发布 Token 发送测试消息，WSS 订阅收到相同消息 ID，并在本地服务中生成投递记录。
此验证不代表 JBM Push 新版本已经部署到生产环境，也不代表已测量手机耗电。

后续于 2026-09-13 完成生产部署：Push 与飞鸽前端滚动更新完成，MySQL 已补充
`extend_data` 字段。默认 ntfy 配置 ID 为 `1789310889810`，主题为 `dangxiao-alerts`。
线上队列投递、消息记录持久化和 WSS 接收均已通过，测试回执 ID：`g68XfF56FwoW`。
管理入口：https://feige.hz-aitech.com/jbm/messages/channels （需登录）。
