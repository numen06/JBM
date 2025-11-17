# OAuth2 登录页面使用说明

## 概述

已为 OAuth2 授权流程创建了一个类似 GitHub 风格的登录页面，符合 OAuth2 标准规范。

## 功能特性

### 1. 登录流程
- ✅ 符合 OAuth2 授权码模式规范
- ✅ GitHub 风格的现代化 UI 设计
- ✅ 深色主题，优秀的视觉体验
- ✅ 响应式设计，支持移动端
- ✅ 完整的错误提示机制
- ✅ 加载动画和禁用状态处理
- ✅ 自动保持 OAuth2 参数传递

### 2. 页面组成

#### oauth2_login.html
- **用户名输入框**：支持用户名或邮箱地址
- **密码输入框**：安全的密码输入
- **登录按钮**：提交表单进行登录
- **客户端信息展示**：显示 OAuth2 客户端 ID
- **错误提示区域**：展示登录失败的错误信息

#### oauth2_authorize.html
- **授权确认页面**：登录成功后显示
- **权限说明**：告知用户授予的权限
- **允许/拒绝按钮**：用户确认授权

## 使用流程

### 标准 OAuth2 授权码流程

```
1. 客户端发起授权请求
   GET /oauth2/authorize?response_type=code&client_id=xxx&redirect_uri=xxx&scope=xxx&state=xxx

2. 系统检查用户登录状态
   - 未登录：显示登录页面 (oauth2_login.html)
   - 已登录：跳转到授权确认页面 (oauth2_authorize.html)

3. 用户在登录页面输入凭证
   POST /oauth2/doLogin
   - username: 用户名
   - password: 密码
   - 携带所有 OAuth2 参数

4. 登录成功后自动跳转到授权页面

5. 用户确认授权
   POST /oauth2/doConfirm

6. 系统生成授权码并重定向回客户端
   redirect_uri?code=xxx&state=xxx

7. 客户端使用授权码换取访问令牌
   POST /oauth2/token
   - grant_type: authorization_code
   - code: 授权码
   - client_id: 客户端ID
   - redirect_uri: 回调地址
```

## API 端点

### 1. GET /oauth2/authorize
**授权端点**
- 检查用户登录状态
- 未登录显示登录页面
- 已登录显示授权确认页面

**参数：**
- `response_type`: 响应类型（通常为 "code"）
- `client_id`: 客户端ID（必填）
- `redirect_uri`: 授权后的回调地址
- `scope`: 请求的权限范围
- `state`: 防 CSRF 攻击的状态参数

### 2. POST /oauth2/doLogin
**登录端点**
- 处理用户登录请求
- 验证用户凭证
- 登录成功后重定向到授权页面

**参数：**
- `username`: 用户名
- `password`: 密码
- 所有来自 authorize 的 OAuth2 参数

### 3. POST /oauth2/doConfirm
**授权确认端点**
- 用户确认授权
- 生成授权码
- 重定向回客户端

### 4. POST /oauth2/token
**令牌端点**
- 使用授权码换取访问令牌

## 设计特点

### 视觉设计
- **配色方案**：
  - 背景色：#0d1117（深色背景）
  - 卡片背景：#161b22（深灰色）
  - 主色调：#238636（绿色按钮）
  - 链接色：#58a6ff（蓝色）

- **交互反馈**：
  - 输入框聚焦效果
  - 按钮悬停状态
  - 加载动画
  - 错误提示

### 安全性
- ✅ 防 CSRF 攻击（state 参数）
- ✅ 密码字段安全输入
- ✅ HTTPS 传输（推荐）
- ✅ 会话管理（Sa-Token）

## 代码修改说明

### OAuth2ServerController.java

#### 修改的方法：

1. **authorize()** - 添加登录状态检查
```java
@GetMapping("/authorize")
public String authorize(...) {
    // 检查用户是否已登录
    if (!StpUtil.isLogin()) {
        return "oauth2_login";  // 未登录显示登录页
    }
    return "oauth2_authorize";  // 已登录显示授权页
}
```

2. **doLogin()** - 处理登录并重定向
```java
@PostMapping("/doLogin")
public Object doLogin(AuthorizeForm authorizeForm, HttpServletResponse response) {
    // 验证用户凭证
    ResultBody<JbmLoginUser> loginResult = sysLoginService.login(...);
    
    // 登录成功后重定向到授权页面
    response.sendRedirect("/oauth2/authorize?...");
}
```

### AuthorizeForm.java

添加了 `state` 字段以支持 OAuth2 规范：
```java
@ApiModelProperty("状态码")
private String state;
```

## 测试示例

### 浏览器测试
```
访问：http://localhost:8080/oauth2/authorize?response_type=code&client_id=test_client&redirect_uri=http://localhost:3000/callback&scope=user&state=random_state
```

### cURL 测试
```bash
# 1. 获取登录页面
curl -v http://localhost:8080/oauth2/authorize?response_type=code&client_id=test_client&redirect_uri=http://localhost:3000/callback

# 2. 提交登录
curl -X POST http://localhost:8080/oauth2/doLogin \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin&password=123456&response_type=code&client_id=test_client&redirect_uri=http://localhost:3000/callback"
```

## 注意事项

1. **Thymeleaf 配置**：确保项目中已配置 Thymeleaf 模板引擎
2. **静态资源**：HTML 页面使用内联 CSS，无需额外静态资源
3. **跨域问题**：如需支持跨域，请配置 CORS
4. **HTTPS**：生产环境建议使用 HTTPS 协议
5. **客户端注册**：确保 OAuth2 客户端已在系统中注册

## 后续扩展

可以考虑添加以下功能：

- [ ] 第三方登录（GitHub、微信、QQ等）
- [ ] 记住我功能
- [ ] 验证码支持
- [ ] 手机号登录
- [ ] 扫码登录
- [ ] 多因素认证（MFA）
- [ ] 密码找回功能
- [ ] 注册页面优化

## 技术栈

- **后端框架**：Spring Boot
- **OAuth2 框架**：Sa-Token OAuth2
- **模板引擎**：Thymeleaf
- **前端样式**：纯 CSS（无依赖）
- **前端交互**：原生 JavaScript

## 文件清单

```
jbm-cluster-platform-auth/
├── src/main/java/.../controller/
│   └── OAuth2ServerController.java       # 控制器（已修改）
├── src/main/java/.../form/
│   └── AuthorizeForm.java                # 表单对象（已修改）
└── src/main/resources/templates/
    ├── oauth2_login.html                 # 登录页面（新增）
    └── oauth2_authorize.html             # 授权页面（已存在）
```

## 常见问题

### Q1: 为什么登录后没有跳转？
A: 检查 `redirect_uri` 参数是否正确，以及是否已在客户端配置中注册。

### Q2: 如何自定义页面样式？
A: 修改 `oauth2_login.html` 中的 `<style>` 标签内的 CSS 样式。

### Q3: 如何添加验证码？
A: 在表单中添加验证码输入框，并在 `doLogin` 方法中添加验证逻辑。

### Q4: 是否支持多语言？
A: 当前为中文版本，可以使用 Thymeleaf 的国际化功能添加多语言支持。

## 联系方式

如有问题，请联系开发团队或提交 Issue。

