# RBAC 验证流程（超管驱动，非代码种子用户）

## 原则

- **系统启动**：`SystemDataInitializer` 空库创建超级管理员与超管角色。
- **标准资源**：`AdminVueRbacSeedInitializer` 在菜单不足时写入标准侧栏菜单 + 标准按钮（`users_*`、`dict_*` 等），仅授权超管；**不**创建业务测试用户。
- **超管治理**：启动后菜单、按钮、角色授权、用户账号均在管理端由超管维护（`/system/menus`、`/system/actions`、`/system/roles`、`/system/users`）。
- **测试用户**：由超管在界面操作，或运行 `scripts/setup_test_users_via_admin.py` 通过 API 创建。

## 标准操作顺序

1. 启动 Auth / Center / Gateway（profile `jaja7`），前端 `npm run dev`。
2. 使用 **admin** 登录管理端。
3. **菜单管理** / **按钮管理**（可选）→ 调整标准菜单与页内按钮定义。
4. **角色管理** → 新建角色 → 钥匙图标勾选菜单 + 按钮（`ACTION_*`）。
5. **用户管理** → 新建用户 → 填写手机/邮箱 → 勾选角色 → 保存（自动注册多凭证）。
5. 退出后用新用户或手机/邮箱登录，对比侧栏与页内按钮。

## 运维脚本（推荐）

Java 用 VS Code 启动 **jaja7: Auth + Center + Gateway**；重复性检测与测试用 Python：

```bash
python scripts/jbm_cluster_ops.py status
python scripts/jbm_cluster_ops.py wait --timeout 90
python scripts/jbm_cluster_ops.py workflow --password Admin@123
```

`workflow` = 检测 → 等待 → 超管登录 → setup-rbac（两角色两用户）→ test-rbac（断言报告）。

分步：

```bash
python scripts/jbm_cluster_ops.py setup-rbac
python scripts/jbm_cluster_ops.py test-rbac
python scripts/test_multi_credential_login.py
```

报告输出：`docs/testing/auth-rest-jaja7/rbac-compare-test-report.md`

## 断言对照（脚本造数后）

| 用户 | 菜单 | 按钮 | 多凭证 |
|------|------|------|--------|
| admin | 全部 | 全部 ACTION_* | admin |
| demo | 仪表盘、字典、审计日志 | dict_view/add，无 delete | demo / 13800138000 / demo@jbm.local |
| viewer | 用户管理 | users_view/edit，无 add/delete | viewer / 13900139000 / viewer@jbm.local |
