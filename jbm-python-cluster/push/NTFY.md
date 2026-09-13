# ntfy 接入 JBM 标准推送

ntfy 是 JBM 的推送渠道，沿用原有收件人解析、消息队列、投递记录与消息中心。
业务调用无需维护 ntfy topic，不使用项目专属主题作为默认收件地址。

## 配置与路由

管理入口：https://feige.hz-aitech.com/jbm/messages/channels （需登录）。
配置类型为 8，填写服务地址、主题前缀、发布 Token、优先级并启用。
线上前缀为 `jbm`，服务为 `https://notify.hz-aitech.com`。

| JBM 收件人 | ntfy topic |
| --- | --- |
| `recUserId = 123` | `jbm-user-123` |
| `recUserIds = [123, 456]` | 分别发往 `jbm-user-123` 与 `jbm-user-456` |
| `recUserId = 0` | `jbm-broadcast` |
| 未指定收件人 | 按标准发送入口发送给当前登录用户 |

重复收件人去重；广播 0 不会回退为当前用户。队列消息携带多人列表时也按人拆分。
消息中的 `topic` 覆盖被拒绝，避免绕过 JBM 收件人路由。主题前缀可在渠道配置中调整。

## 标准发送

沿用 `POST /push/pushMessage/sendPushMsg` 或 `POST /push/pushTest/send`，网关基础路径按环境添加：

```json
{
  "pushWay": "ntfy",
  "recUserIds": [123, 456],
  "title": "运行通知",
  "content": "任务已完成",
  "showInMessageCenter": true
}
```

现有发送测试页面可以选择 ntfy，收件人仍从 JBM 用户池选择。
`POST /push/notification/send/ntfy` 同样使用标准收件人路由，可通过 `syncDelivery: true`
获取即时发送结果；默认使用 RabbitMQ。`ntfyConfigId` 可选，用于选择已启用渠道配置。
`priority` 可按消息覆盖。

每个收件人分别记录成功或失败、ntfy 消息 ID、主题和回执状态。
多人发送中有失败时，总结果不会标为全部成功。
`sent` 表示 ntfy 已接收，不代表手机已展示或用户已读。

## 订阅权限

手机可以通过 WSS 订阅自己的用户主题；需要广播通知时再订阅广播主题。
HTTPS 发布与 WSS 订阅互不冲突，手机中的服务器地址仍填写 HTTPS 地址。

JBM 收件人负责发送路由，ntfy 账号 ACL 负责读取权限。个人 topic 名称本身不是密码。
ntfy 账号需授予对应个人主题的只读权限；不要为普通用户开放 `jbm-user-*` 读取权限，
也不要共享管理员账号。本次未自动创建或同步 JBM 用户对应的 ntfy 登录账号。

发布 Token 仅用于写入，不能订阅。管理 API 返回 Token 掩码，原样保存会保留真实值。
发布账号应具有 `<前缀>-user-*` 和 `<前缀>-broadcast` 的写权限。

## 数据库与验证

旧 MySQL 消息表必须有 `extend_data` 或 `extend` 字段以保存回执。
两者都不存在时，执行 `migrations/20260913_ntfy_delivery_details.sql` 并重启 Push。
迁移仅增加可空字段，应用回滚时保留字段即可。

运行 `.venv/Scripts/python.exe -m pytest push/tests/test_ntfy.py push/tests/test_push_compat.py -q`。
2026-09-13 修正版：33 项后端测试通过，JBM 和飞鸽前端构建通过。
线上验证标准发送入口 → RabbitMQ → Push 消费者 → 三个独立 topic 的 WSS 接收和消息记录通过。
旧版 `dangxiao-alerts` 默认主题已从 JBM 配置移除。
