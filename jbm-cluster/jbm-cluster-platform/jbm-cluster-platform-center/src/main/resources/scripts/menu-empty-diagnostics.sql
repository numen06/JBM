-- 菜单接口返回空时的自检脚本（对应 /current/user/menu 后端逻辑）
-- 在目标库执行；将占位符替换为实际登录 user_id、会话中的 app_id（与 JbmLoginUser.appId 一致，非 URL 参数）
-- H2 本地：http://localhost:7777/h2-console ，种子超管 user_id = 1（JbmConstants.ROOT_USER_ID），user_name = admin

-- 1) 用户是否有角色
SELECT * FROM base_role_user WHERE user_id = ?;

-- 2) 非超管：角色授权且权限启用（与 BaseAuthorityRoleMapper 一致）
SELECT a.authority_id, a.authority, a.status, m.menu_id, m.menu_name, m.app_id
FROM base_authority_role ar
JOIN base_authority a ON ar.authority_id = a.authority_id
JOIN base_menu m ON a.menu_id = m.menu_id
WHERE ar.role_id IN (SELECT role_id FROM base_role_user WHERE user_id = ?)
  AND a.status = 1;

-- 3) 超管 / 全量菜单路径：与 BaseAuthorityMapper.selectAuthorityMenu 在「不按 app 过滤」时等价的核心 JOIN
--    若此处无行，说明 base_authority 与 base_menu 无法关联或表为空
SELECT a.authority_id, a.authority, a.status, m.menu_id, m.menu_name, m.app_id
FROM base_authority a
INNER JOIN base_menu m ON a.menu_id = m.menu_id;

-- 4) 按登录态 appId 过滤（与默认 selectAuthorityMenu 一致；appId 为 NULL 时改为 AND m.app_id IS NULL）
-- SELECT ... FROM base_authority a INNER JOIN base_menu m ON a.menu_id = m.menu_id
-- WHERE (m.app_id = ? OR m.app_id IS NULL);

-- 可选：新库导入后若普通用户需菜单，可酌情启用菜单与菜单权限（请在业务确认后取消注释执行）
-- UPDATE base_menu SET status = 1 WHERE status = 0;
-- UPDATE base_authority SET status = 1 WHERE menu_id IS NOT NULL AND status = 0;
