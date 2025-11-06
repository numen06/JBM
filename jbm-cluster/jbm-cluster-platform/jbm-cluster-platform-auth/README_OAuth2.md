# OAuth2 登录页面 - 完整指南

## 📌 快速导航

- **快速开始**: 查看 [`快速开始.md`](./快速开始.md)
- **详细说明**: 查看 [`OAuth2登录页面说明.md`](./OAuth2登录页面说明.md)
- **更新日志**: 查看 [`更新日志.md`](./更新日志.md)

## 🎯 功能特性

### ✅ 已完成功能

1. **GitHub 风格登录页面** (`oauth2_login.html`)
   - 深色主题，现代化 UI
   - 响应式设计，支持移动端
   - 完整的错误提示和加载状态

2. **OAuth2 授权确认页面** (`oauth2_authorize.html`)
   - 清晰的权限说明
   - 允许/拒绝操作

3. **客户端演示页面** (`oauth2_client_demo.html`)
   - 可视化配置 OAuth2 参数
   - 一键测试授权流程
   - 完整的流程说明

4. **完整的授权流程**
   - 符合 OAuth2 授权码模式规范
   - 自动检测登录状态
   - 保持所有 OAuth2 参数传递

## 🏗️ 架构设计

### 职责分离

```
┌─────────────────────────────────────────────────────────┐
│                    前端页面                              │
│  oauth2_login.html  │  oauth2_authorize.html  │ demo   │
└──────────────┬──────────────────────────┬───────────────┘
               │                          │
               │ 页面跳转                 │ AJAX 请求
               ↓                          ↓
┌──────────────────────────┐  ┌─────────────────────────┐
│    LoginController       │  │  OAuth2ServerController │
│    (@Controller)         │  │  (@RestController)      │
├──────────────────────────┤  ├─────────────────────────┤
│ • /oauth2/authorize      │  │ • /oauth2/doLogin       │
│ • /oauth2/demo           │  │ • /oauth2/token         │
│ • /login                 │  │ • /oauth2/doConfirm     │
│                          │  │ • /oauth2/userinfo      │
│ 返回: 视图名称 (String)   │  │ 返回: JSON (ResultBody) │
└──────────────────────────┘  └─────────────────────────┘
```

### 设计原则

1. **Controller**: 页面跳转，返回视图名称
2. **RestController**: API 接口，返回 JSON 数据
3. **前后端分离**: API 返回数据，前端控制跳转

## 🚀 快速开始

### 1. 启动应用

```bash
cd jbm-cluster-platform-auth
mvn spring-boot:run
```

### 2. 访问演示页面

```
http://localhost:8080/oauth2/demo
```

### 3. 测试流程

1. 点击"发起 OAuth2 授权请求"
2. 输入用户名和密码
3. 点击"允许"授权
4. 获取授权码

## 📚 API 端点

| 端点 | 类型 | 控制器 | 返回 | 说明 |
|------|------|--------|------|------|
| `/oauth2/authorize` | GET | LoginController | HTML | 授权页面（含登录） |
| `/oauth2/demo` | GET | LoginController | HTML | 演示页面 |
| `/oauth2/doLogin` | POST | OAuth2ServerController | JSON | 登录处理 |
| `/oauth2/token` | POST | OAuth2ServerController | JSON | 获取令牌 |
| `/oauth2/doConfirm` | POST | OAuth2ServerController | JSON | 授权确认 |
| `/oauth2/userinfo` | GET | OAuth2ServerController | JSON | 获取用户信息 |

## 🎨 页面展示

### 登录页面 (`/oauth2/authorize`)

- 🌙 深色主题设计
- 📱 完全响应式
- ⚡ 流畅动画效果
- 🔒 安全的密码输入
- 📊 OAuth2 客户端信息展示

### 授权确认页面

- ✅ 清晰的权限说明
- 🎯 允许/拒绝操作
- 🔐 符合 OAuth2 规范

### 演示页面 (`/oauth2/demo`)

- 🎮 交互式配置界面
- 📋 完整流程说明
- 🚀 一键测试功能

## 🔧 配置说明

### Thymeleaf 配置

确保 `application.yml` 中有以下配置：

```yaml
spring:
  thymeleaf:
    mode: HTML
    encoding: UTF-8
    cache: false  # 开发环境
    prefix: classpath:/templates/
    suffix: .html
```

### Sa-Token OAuth2 配置

```java
@Bean
public SaOAuth2Config oauth2Config() {
    SaOAuth2Config config = new SaOAuth2Config();
    config.setAllowUrl("/oauth2/*");
    config.setIsCode(true);  // 开启授权码模式
    return config;
}
```

## 📝 代码示例

### 发起授权请求

```javascript
// 前端代码
const authUrl = '/oauth2/authorize'
    + '?response_type=code'
    + '&client_id=test_client'
    + '&redirect_uri=' + encodeURIComponent('http://localhost:3000/callback')
    + '&scope=user'
    + '&state=random123';

window.location.href = authUrl;
```

### 处理登录

```javascript
// 登录表单提交
const formData = new FormData();
formData.append('username', 'admin');
formData.append('password', '******');
formData.append('response_type', 'code');
formData.append('client_id', 'test_client');
// ... 其他参数

const response = await fetch('/oauth2/doLogin', {
    method: 'POST',
    body: formData
});

const result = await response.json();
if (result.success) {
    // 跳转到授权页面
    window.location.href = result.result;
}
```

### 用授权码换取令牌

```bash
curl -X POST http://localhost:8080/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code" \
  -d "code=YOUR_AUTH_CODE" \
  -d "client_id=test_client" \
  -d "redirect_uri=http://localhost:3000/callback"
```

## 🔐 安全特性

- ✅ **State 参数验证** - 防止 CSRF 攻击
- ✅ **密码安全输入** - 使用 password 类型
- ✅ **会话管理** - 基于 Sa-Token
- ✅ **授权码时效** - 授权码具有过期时间
- ✅ **HTTPS 支持** - 生产环境推荐

## 🐛 故障排查

### 问题 1: 页面显示 JSON 而不是 HTML

**原因**: 方法在 RestController 中  
**解决**: 将方法移至 Controller 类

### 问题 2: 登录后没有跳转

**原因**: 前端未正确处理 JSON 响应  
**解决**: 检查 JavaScript，确保读取 `result.result` 或 `result.data`

### 问题 3: OAuth2 参数丢失

**原因**: 表单缺少隐藏字段  
**解决**: 确保所有 OAuth2 参数都作为隐藏字段传递

### 问题 4: 授权码无效

**原因**: 授权码已过期或已使用  
**解决**: 授权码只能使用一次，需要重新发起授权

## 📦 文件清单

```
jbm-cluster-platform-auth/
├── src/main/java/
│   └── com/jbm/cluster/auth/controller/
│       ├── LoginController.java          ✅ 新增方法
│       └── OAuth2ServerController.java   ✅ 修改方法
├── src/main/resources/templates/
│   ├── oauth2_login.html                 ✅ 新增
│   ├── oauth2_authorize.html             ✅ 已存在
│   └── oauth2_client_demo.html           ✅ 新增
├── OAuth2登录页面说明.md                  📖 详细说明
├── 快速开始.md                           📖 快速指南
├── 更新日志.md                           📖 更新日志
└── README_OAuth2.md                      📖 本文件
```

## 🎯 下一步

现在您可以：

1. ✅ 访问 `/oauth2/demo` 体验完整流程
2. ✅ 自定义登录页面样式
3. ✅ 集成第三方登录（GitHub、微信等）
4. ✅ 添加验证码功能
5. ✅ 优化移动端体验
6. ✅ 添加多语言支持

## 📞 技术支持

- **详细文档**: 查看 `OAuth2登录页面说明.md`
- **快速开始**: 查看 `快速开始.md`
- **更新日志**: 查看 `更新日志.md`

## 🎉 开始使用

```bash
# 1. 启动应用
mvn spring-boot:run

# 2. 打开浏览器
http://localhost:8080/oauth2/demo

# 3. 开始测试！
```

祝您使用愉快！🚀

