# 用户常规操作与权限 REST 测试 (jaja7)

## 执行

```bash
python scripts/run_user_perm_rest_tests.py --wait 30
python scripts/run_user_perm_rest_tests.py --base-url http://127.0.0.1:7777 --auth-url http://127.0.0.1:5555
```

## 用例配置

- scripts/user_perm_rest_modules.json

## 前置

Gateway(7777)、Center、Auth(5555)，账号 admin/admin123，OAuth client demo/demo123。