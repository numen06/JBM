# 对话式参数补全 - 测试场景

## 测试环境配置

### 1. 启用对话模式
在 `application.yml` 或 `bootstrap.yml` 中添加：

```yaml
agent:
  dialogue:
    enabled: true              # 启用对话模式
    max-rounds: 5              # 最大对话轮次
    session-timeout-hours: 24  # 会话超时（小时）
    auto-infer: true           # 自动推断参数
    extraction-confidence-threshold: 0.5  # 参数提取置信度阈值
```

### 2. 确保必要服务已启动
- Nacos 注册中心
- 至少一个提供 API 的微服务（包含 Swagger/OpenAPI 文档）
- 通义千问 API Key 已配置

## 测试场景

### 场景 1：基础参数补全（单参数）

**目标**：验证单个必填参数的收集流程

**测试步骤**：
1. 用户发送："查询用户信息"
2. 系统识别意图并选择 API：`GET /api/user/{userId}`
3. 系统发现缺少必填参数 `userId`
4. 系统询问："[1/1] 请提供用户ID（类型：integer）"
5. 用户回复："123"
6. 系统提取参数：`userId = 123`
7. 系统执行 API 并返回结果

**预期输出流**：
```json
{"type": "sessionId", "sessionId": "xxx"}
{"type": "stage", "stage": "nlu", "message": "正在理解您的问题..."}
{"type": "intent", "name": "query_user_info", "type": "QUERY", ...}
{"type": "stage", "stage": "routing", ...}
{"type": "stage", "stage": "selection", ...}
{"type": "parameterNeeded", "parameter": "userId", "question": "[1/1] 请提供用户ID（类型：integer）", ...}
{"type": "text", "content": "[1/1] 请提供用户ID（类型：integer）"}
[DONE]
```

**第二轮交互**（用户回复 "123"）：
```json
{"type": "sessionId", "sessionId": "xxx"}
{"type": "parameterCollected", "parameter": "userId", "value": 123}
{"type": "apiReady", "message": "参数已齐全，开始执行..."}
{"type": "apiCalling", "url": "feign://user-service/api/user/123", ...}
{"type": "apiResult", "statusCode": 200, ...}
{"type": "stage", "stage": "formatting", ...}
{"type": "text", "content": "用户ID 123 的信息如下..."}
[DONE]
```

---

### 场景 2：多参数收集

**目标**：验证多个必填参数的逐个收集

**测试步骤**：
1. 用户："发送邮件"
2. 系统识别 API：`POST /api/email/send`，需要参数：`toEmail`, `subject`, `content`
3. 系统询问第一个参数："[1/3] 请提供收件人邮箱（类型：string）"
4. 用户："test@example.com"
5. 系统询问第二个参数："[2/3] 请提供邮件主题（类型：string）"
6. 用户："测试邮件"
7. 系统询问第三个参数："[3/3] 请提供邮件内容（类型：string）"
8. 用户："这是一封测试邮件"
9. 系统执行 API 并返回成功消息

**验证点**：
- 参数按顺序逐个询问
- 进度提示 `[x/y]` 正确显示
- 每个参数都被正确提取和保存

---

### 场景 3：参数验证失败重试

**目标**：验证参数验证失败后的重新询问机制

**测试步骤**：
1. 用户："创建订单"
2. 系统询问："[1/2] 请提供产品ID（类型：integer）"
3. 用户："abc"（无效的整数）
4. 系统检测到提取失败，重新询问："抱歉，我没有理解您的回复。请提供产品ID（类型：integer）"
5. 用户："123"（有效整数）
6. 系统成功提取，继续下一个参数

**验证点**：
- 无效输入被正确检测
- 系统给出友好的错误提示
- 允许用户重新输入
- 轮次计数正确递增

---

### 场景 4：取消对话

**目标**：验证用户主动取消对话的处理

**测试步骤**：
1. 用户："查询订单"
2. 系统询问："[1/1] 请提供订单ID"
3. 用户："取消"（或 "退出", "算了", "cancel"）
4. 系统返回："已取消操作"
5. 对话状态被清除

**验证点**：
- 取消指令被正确识别
- 对话状态被清理
- 系统返回友好提示

---

### 场景 5：超过最大轮次限制

**目标**：验证超过最大轮次的保护机制

**配置**：设置 `max-rounds: 3`

**测试步骤**：
1. 用户："发送邮件"（需要3个参数）
2. 系统询问第一个参数
3. 用户回复无效内容（触发重试）
4. 系统重新询问第一个参数（轮次 2）
5. 用户再次回复无效内容
6. 系统重新询问（轮次 3）
7. 系统检测到达到最大轮次，返回错误："对话轮次超过限制，请稍后重试"

**验证点**：
- 轮次计数正确
- 超过限制时停止对话
- 对话状态被清除

---

### 场景 6：部分参数已提供

**目标**：验证意图识别时已提取部分参数的处理

**测试步骤**：
1. 用户："查询用户123的订单"
2. 系统识别意图：`query_user_orders`
3. 系统从用户输入中提取参数：`userId = 123`
4. 系统发现 API 需要 `userId` 和 `status` 两个参数
5. 系统只询问缺失的参数："[2/2] 请提供订单状态（类型：string）"
6. 用户："已完成"
7. 系统合并参数并执行 API

**验证点**：
- 已提取的参数不再询问
- 进度提示正确（从 [2/2] 开始）
- 参数正确合并

---

### 场景 7：会话恢复

**目标**：验证跨请求的会话状态保持

**测试步骤**：
1. 用户发送请求（sessionId = "session123"）："发送邮件"
2. 系统询问第一个参数并保存对话状态
3. 用户关闭客户端
4. 用户重新连接，使用相同 sessionId 发送："test@example.com"
5. 系统从对话状态中恢复上下文
6. 系统继续询问第二个参数

**验证点**：
- 对话状态在内存中保持
- 可以跨请求恢复对话
- SessionId 机制工作正常

---

### 场景 8：智能参数推断（可选功能）

**目标**：验证自动参数推断功能

**配置**：`auto-infer: true`

**测试步骤**：
1. 用户："查询我的订单列表"
2. 系统识别 API：`GET /api/orders`，需要参数：`userId`, `pageSize`, `pageNum`
3. 系统自动推断：
   - `userId`: 从上下文获取当前用户ID
   - `pageSize`: 默认值 10
   - `pageNum`: 默认值 1
4. 系统无需询问，直接执行 API

**验证点**：
- 常用参数被自动推断
- 无需用户输入即可执行
- 推断的参数值合理

---

## 测试 API 调用

### 使用 curl 测试

```bash
# 场景 1：基础参数补全
curl -X POST http://localhost:8080/ai/agent/stream \
  -H "Content-Type: application/json" \
  -d '{
    "message": "查询用户信息",
    "sessionId": "test-session-001",
    "enableAgent": true
  }'

# 第二轮：提供参数
curl -X POST http://localhost:8080/ai/agent/stream \
  -H "Content-Type: application/json" \
  -d '{
    "message": "123",
    "sessionId": "test-session-001",
    "enableAgent": true
  }'
```

### 使用前端测试

访问：`http://localhost:8080/ai/chat-ui`

在聊天界面中输入测试场景的用户消息。

---

## 日志观察

启用 DEBUG 日志查看详细处理过程：

```yaml
logging:
  level:
    com.jbm.cluster.ai.agent: DEBUG
    com.jbm.cluster.ai.agent.dialogue: DEBUG
```

**关键日志标记**：
- `💬 [Agent]` - Agent 主流程
- `🔍 [参数收集]` - 参数收集流程
- `📋 [Parameter Collector]` - 参数分析
- `🔮 [参数提取]` - AI 参数提取
- `✅ / ❌` - 成功/失败标记

---

## 常见问题排查

### 1. 对话状态不保持
- 检查 sessionId 是否在多次请求中保持一致
- 检查 DialogueStateManager 是否正常工作
- 查看对话状态是否过期（默认24小时）

### 2. 参数提取失败
- 检查通义千问 API Key 是否有效
- 查看 AI 返回的提取结果日志
- 降低 `extraction-confidence-threshold` 阈值

### 3. 超过轮次限制
- 增加 `max-rounds` 配置
- 优化参数提问的清晰度
- 检查用户输入是否有效

### 4. 找不到匹配的 API
- 确保 ApiMetadataCollector 已收集到 API 元数据
- 检查 API 的 summary 描述是否清晰
- 降低 API Selection 的 `match-threshold`

---

## 性能监控

观察以下指标：
- 对话状态数量：`dialogueStateManager.getActiveStateCount()`
- 平均对话轮次：通过统计信息获取
- 参数提取成功率：统计 extraction success/failure
- API 调用成功率：统计 API execution success/failure

---

## 后续优化方向

1. **智能参数推断增强**
   - 从对话历史中学习用户偏好
   - 支持更多上下文信息源

2. **多语言支持**
   - 支持英文参数收集
   - 国际化提问模板

3. **参数验证增强**
   - 支持正则表达式验证
   - 支持自定义验证规则

4. **对话状态持久化**
   - 支持 Redis 存储对话状态
   - 支持分布式部署

5. **用户体验优化**
   - 支持参数批量提供（一次回复多个参数）
   - 支持参数修改（"修改收件人为..."）
   - 支持对话历史查看

