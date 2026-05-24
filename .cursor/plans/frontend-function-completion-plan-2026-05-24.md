# Frontend Function Completion Plan - 2026-05-24

## Goal

Complete the upgraded admin frontend so the organization, user, app, role, and related system pages are not only buildable, but usable end to end against the verified local backend:

- Gateway: `http://127.0.0.1:7777`
- Frontend dev: `http://127.0.0.1:5173`
- Verified backend smoke: `scripts/run_org_plan_smoke.py` PASS
- Verified backend REST regression: `scripts/run_all_rest_tests.py ...` PASS

Cursor should implement the frontend gaps below, then run frontend build plus real browser/API checks.

## Current Gaps Observed

1. Many system pages contain mojibake Chinese text in labels, descriptions, validation messages, and table headers.
2. Organization support exists but is shallow:
   - `OrgList.vue` is a flat tree table with only basic create/edit/delete.
   - No expand/collapse, parent selection, root/child create affordance, status filter, or refresh after shared org changes.
   - `useOrgTree()` is instantiated separately in `OrgTreeSelect`, `UserList`, and `AppList`, causing duplicate `/baseOrg/tree` calls and stale labels.
3. User management supports `companyId`, `departmentId`, roleIds, and extra orgIds, but UX is incomplete:
   - extra organization authorization is a long checkbox list, poor for large org trees.
   - no dedicated drawer/section to inspect primary org, department, extra org auth, roles, and accounts together.
   - search only uses `keyword`; no status/org filter.
   - create user does not assign roles in the same submit path even though edit does.
4. App management only lists and edits orgId:
   - no filters by org/status/keyword.
   - no clear display for client/secret/api fields where present.
   - no batch refresh or duplicate-safe validation around appCode.
5. Form patterns are inconsistent:
   - destructive actions use native `confirm`.
   - many icon buttons lack title/aria labels.
   - loading/errors are coarse and not always field-specific.
6. There is no frontend E2E/smoke script proving login -> org/user/app pages work in the browser.

## Implementation Tasks

### 1. Fix UI Text and Encoding

Files:

- `jbm-admin-vue/src/views/system/*.vue`
- `jbm-admin-vue/src/api/*.ts`
- `jbm-admin-vue/src/api/types.ts`

Tasks:

- Replace all mojibake strings with correct Chinese text.
- Keep API comments readable or remove comments that add no value.
- Ensure `vue-tsc -b` passes after text replacement.

Acceptance:

- No mojibake remains under `jbm-admin-vue/src`.
- Run: `rg -n "�|鈥|锛|鐢|绠|搴|鏄|缁|鍚|閭|鎵|瑙|鏉|鍓|淇|璇|瀵" jbm-admin-vue/src`
- Command returns no user-facing strings that are still corrupted.

### 2. Make `useOrgTree` a Shared Store

Files:

- `jbm-admin-vue/src/composables/useOrgTree.ts`
- `jbm-admin-vue/src/components/OrgTreeSelect.vue`
- `jbm-admin-vue/src/views/system/UserList.vue`
- `jbm-admin-vue/src/views/system/AppList.vue`
- `jbm-admin-vue/src/views/system/OrgList.vue`

Tasks:

- Convert `useOrgTree()` state to module-level shared refs so all consumers share one loaded tree.
- Add `refreshOrgs()` and `ensureOrgsLoaded()` helpers.
- Add `orgOptions`, `childrenByParent`, and `orgPathLabel(id)` helpers.
- After create/update/delete org, refresh the shared org tree so User/App labels update.

Acceptance:

- Opening User and App pages should not trigger duplicate org tree loads from every select instance.
- Updating an organization name and returning to User/App should show updated labels without full page reload.

### 3. Complete Organization Management Page

File:

- `jbm-admin-vue/src/views/system/OrgList.vue`

Tasks:

- Replace flat-only view with a tree table supporting expand/collapse all and per-node expand.
- Add actions:
  - create root org
  - create child org from selected row
  - edit row
  - delete row with guarded confirmation
- Form fields:
  - orgName required
  - parentId via `OrgTreeSelect`
  - orgCode
  - orgType
  - managerId
  - status
- Add filters:
  - keyword
  - status
- Disable deleting rows with children unless backend explicitly supports cascade.
- Show leaf path/path label when available.

Acceptance:

- Can create a child organization under default org.
- Tree refreshes and child appears nested.
- Can edit orgName/status and see updated row.
- Delete action is guarded and refreshes tree.

### 4. Complete User Management Page

File:

- `jbm-admin-vue/src/views/system/UserList.vue`

Tasks:

- Add filters:
  - keyword
  - status
  - primary organization/companyId
  - departmentId
- Add visible columns:
  - primary org
  - department
  - userType
  - roles summary
  - account count or account badges where loaded
- Improve edit/create dialog:
  - assign roles on create as well as edit if backend accepts `roleIds`; if backend does not, create then call role assignment endpoint.
  - replace extra org checkbox wall with a tree-style multi-select or compact searchable panel.
  - clearly separate primary org, department, and cross-org data authorization.
  - show selected extra orgs as removable chips.
- Add user detail drawer:
  - basic info
  - login accounts
  - roles
  - org authorizations
- Add account actions if supported by current APIs:
  - activate email/mobile
  - show account status.

Acceptance:

- Create user with primary org, department, roles, and extra orgs.
- Reopen edit dialog and see roles and extra orgs preselected.
- Save edits and verify via backend:
  - `GET /user/{id}/roles`
  - `GET /user/{id}/orgs`
- User list filters narrow results without breaking pagination.

### 5. Complete App Management Page

File:

- `jbm-admin-vue/src/views/system/AppList.vue`

Tasks:

- Add filters:
  - keyword/appName/appCode
  - orgId
  - status
- Add visible columns where available:
  - appId
  - appName
  - appCode
  - clientId
  - apiKey if returned
  - org path label
  - status
  - create/update time if type supports it
- Improve form:
  - org required
  - appName/appCode required
  - status explicit
  - keep clientId optional if backend generates it
- Add detail drawer for app identity and org binding.

Acceptance:

- Create app bound to a newly created org.
- App list displays org label/path.
- Editing orgId moves the app to another org and list refreshes correctly.

### 6. Standardize CRUD Interaction

Files:

- `jbm-admin-vue/src/components/CrudDialog.vue`
- Add a reusable confirm dialog if no existing component exists.
- Touch system pages using native `confirm`.

Tasks:

- Replace native `confirm()` with app-styled confirm modal.
- Add `title`/`aria-label` to icon-only buttons.
- Ensure destructive buttons use consistent labels and loading state.
- Make table empty/error/loading states consistent.

Acceptance:

- No `confirm(` remains in `jbm-admin-vue/src/views`.
- Icon-only buttons are accessible.
- Save/delete failures surface backend message.

### 7. Add Frontend Smoke Automation

Preferred tool:

- Playwright, if already available in repo. If not available, add a small script that can use installed browser tooling without changing production deps unnecessarily.

Suggested file:

- `jbm-admin-vue/scripts/smoke-admin.mjs`

Flow:

1. Open `http://127.0.0.1:5173/login`.
2. Log in with `admin / Admin@123` using current login UI.
3. Navigate to:
   - `/system/orgs`
   - `/system/users`
   - `/system/apps`
4. Assert each page renders:
   - page title
   - table/list
   - create button where permission allows
5. Optionally create a test org/app with timestamp and verify it appears.

Acceptance:

- Script exits 0 when backend/frontend are running.
- Save screenshots to `.cursor/logs/frontend-smoke/`.

## Required Verification Commands

Run these after implementation:

```powershell
cd D:\workspaces\JBM7
curl.exe -s http://127.0.0.1:5555/actuator/health
curl.exe -s http://127.0.0.1:8888/actuator/health
curl.exe -s http://127.0.0.1:7777/actuator/health
$env:LOGIN_PASSWORD='Admin@123'; python scripts\run_org_plan_smoke.py
$env:LOGIN_PASSWORD='Admin@123'; python scripts\run_all_rest_tests.py --profile jaja7 --wait 60 --base-url http://127.0.0.1:7777 --auth-url http://127.0.0.1:5555
cd D:\workspaces\JBM7\jbm-admin-vue
npm.cmd run build
```

Then run the frontend browser smoke:

```powershell
cd D:\workspaces\JBM7\jbm-admin-vue
npm.cmd run dev -- --host 127.0.0.1 --port 5173
node scripts\smoke-admin.mjs
```

## Definition of Done

- Backend REST remains PASS.
- Frontend build remains PASS.
- Browser smoke proves login and organization/user/app pages render and basic flows work.
- No mojibake in user-facing source strings.
- Organization data is represented consistently across Org, User, and App pages.
