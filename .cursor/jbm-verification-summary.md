# JBM 本轮验证与完善记录

时间：2026-05-25

## 已修复

- 后端分布式信任边界：外部请求只要携带 `Authorization`，就必须走用户 Token 校验，不能因为伪造内部服务头绕过鉴权。
- 首页重新设计为开源社区平台入口，补充 JBM OpenAPI、API Key、租户隔离、子应用接入和加密传输主线。
- 登录/注册品牌侧栏改为开发者接入流程视图，弱化单色宣传感，突出 OAuth2、Gateway/Auth/Center 分层和 OpenAPI 签名访问。
- API Wiki 补充从注册登录到子应用、API Key、签名调用、租户隔离的用户接入路径。

## 实测结果

- 后端集群重启：Auth `5555`、Center `8888`、Gateway `7777` 均启动成功。
- Feign/内部信任回归：`trust 6/6 PASS`，之前的坏 Bearer + 内部头绕过场景已被拒绝。
- API Key 用户接入全链路：注册、开发者申请、审批、授权、创建应用、个人 Key、应用 Key、client_token、签名调用、越权拒绝全部 PASS。
- 全量 REST 回归：Auth、Center、Feign Trust、User Permission 全部 PASS。
- 前端构建：`npm run build` PASS，仅保留 Vite 动态导入 chunk 提示。
- 浏览器实测：`/`、`/login`、`/register`、`/docs` 内容命中预期；登录页使用默认 admin 凭证点击登录后跳转 `/dashboard`。

## 截图

- `.cursor/screenshots/landing-after.png`
- `.cursor/screenshots/login-after.png`
- `.cursor/screenshots/register-after.png`
- `.cursor/screenshots/docs-after.png`

## 后续建议

- 如果要进入生产级 OpenAPI，建议把 Wiki 中的签名 canonical string 与后端验签实现生成同源文档，避免文档与代码漂移。
- 为多租户数据隔离补充独立自动化用例：不同租户用户、应用、API Key 互相不可见且不可授权。
