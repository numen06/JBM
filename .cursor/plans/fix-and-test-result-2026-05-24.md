# Fix and Test Result - 2026-05-24

## Fixes applied

1. Center runtime: added `mysql:mysql-connector-java` runtime dependency to `jbm-cluster-platform-center`, fixing `Cannot load driver class: com.mysql.cj.jdbc.Driver`.
2. Liquibase org migration: added legacy `base_org.status` patch and new `V15__base_user_org_master_columns.sql`; included V15 in `db.changelog-master.yaml`, fixing `base_user_org` missing `code/app_id/parent_id/level/leaf_path/extend_data`.
3. Gateway jaja7 route/security: added `/user/sessions` to sign/auth ignores and added `/customForms/**,/customFormsItem/**` to the center route predicate.
4. Test runners: fixed center REST Authorization header to use `Bearer`; added admin login for center REST; added gateway admin token for auth-suite gateway lookup steps.
5. Test data: changed center REST role/extend/custom-form names to ASCII to avoid encoding-sensitive assertions.

## Executed verification

| Check | Result |
|------|--------|
| Auth health `:5555` | PASS |
| Center health `:8888` | PASS |
| Gateway health `:7777` | PASS |
| `python scripts\run_org_plan_smoke.py` | PASS: login, org tree, default org, user list, app orgId, create org, create app with orgId, user org query, current user |
| `python scripts\run_all_rest_tests.py --profile jaja7 --wait 60 --base-url http://127.0.0.1:7777 --auth-url http://127.0.0.1:5555` | PASS |
| Frontend `npm.cmd run build` in `jbm-admin-vue` | PASS |
| Frontend dev `/login` at `http://127.0.0.1:5173/login` | PASS: HTTP 200 |

## Final local service state

| Service | Port | State |
|------|------|-------|
| Auth | 5555 | UP |
| Center | 8888 | UP |
| Gateway | 7777 | UP |
| Frontend Vite | 5173 | UP |
