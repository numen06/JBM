package com.jbm.cluster.common.mysql.init;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.constants.ResourceType;
import com.jbm.cluster.api.entitys.basic.*;
import com.jbm.cluster.api.model.auth.OpenAuthority;
import com.jbm.cluster.common.mysql.mapper.BaseActionMapper;
import com.jbm.cluster.common.mysql.mapper.BaseMenuMapper;
import com.jbm.cluster.common.mysql.mapper.BaseUserMapper;
import com.jbm.cluster.common.mysql.service.BaseAuthorityService;
import com.jbm.cluster.common.mysql.service.BaseRoleService;
import com.jbm.cluster.core.constant.JbmConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 首次启动补全<strong>标准菜单 + 标准按钮</strong>（与 Vue 路由、页内 ACTION_* 对齐），并仅授权给超管角色。
 * <p>
 * 之后由超级管理员在「菜单管理」「按钮管理」「角色管理」中增删改；本类不会覆盖已存在数据。
 * 不创建业务用户/业务角色（见 {@link SystemDataInitializer} 仅初始化超管）。
 */
@Slf4j
@Component
@Order(Integer.MAX_VALUE - 90)
@ConditionalOnProperty(name = "jbm.cluster.data-init.vue-rbac-metadata-enabled", havingValue = "true", matchIfMissing = true)
public class AdminVueRbacSeedInitializer implements ApplicationRunner {

    private static final String MARKER_KEY = "admin_vue_rbac_v1";

    private static final long MENU_PLATFORM = 100L;
    private static final long MENU_SYSTEM = 101L;
    private static final long MENU_GATEWAY = 120L;
    private static final long MENU_OTHER = 130L;
    private static final long MENU_MESSAGES = 150L;
    private static final long MENU_JOBS = 160L;
    private static final long MENU_DOCS = 170L;

    private static final long MENU_DASHBOARD = 110L;
    private static final long MENU_USERS = 102L;
    private static final long MENU_ROLES = 103L;
    private static final long MENU_MENUS = 106L;
    private static final long MENU_ACTIONS = 112L;
    private static final long MENU_ORGS = 107L;
    private static final long MENU_AUTHORITY = 104L;
    private static final long MENU_APPS = 108L;
    private static final long MENU_DICTS = 109L;
    private static final long MENU_EXTEND = 111L;
    private static final long MENU_GW_ROUTE = 121L;
    private static final long MENU_GW_RATE = 122L;
    private static final long MENU_GW_IP = 123L;
    private static final long MENU_LOGS = 131L;
    private static final long MENU_DEVELOPER = 105L;
    private static final long MENU_API_KEY = 113L;
    private static final long MENU_ONLINE_USERS = 114L;
    private static final long MENU_API_MGMT = 140L;
    private static final long MENU_API_REGISTRY = 141L;
    private static final long MENU_API_DOCS = 142L;
    private static final long MENU_MESSAGE_CENTER = 151L;
    private static final long MENU_MESSAGE_SEND_TEST = 152L;
    private static final long MENU_MESSAGE_CHANNELS = 153L;
    private static final long MENU_JOB_LIST = 161L;
    private static final long MENU_JOB_LOGS = 162L;
    private static final long MENU_DOC_FILES = 171L;

    private static final long ACTION_USERS_VIEW = 2001L;
    private static final long ACTION_USERS_ADD = 2002L;
    private static final long ACTION_USERS_EDIT = 2003L;
    private static final long ACTION_USERS_DELETE = 2004L;
    private static final long ACTION_DICT_VIEW = 2101L;
    private static final long ACTION_DICT_ADD = 2102L;
    private static final long ACTION_DICT_EDIT = 2103L;
    private static final long ACTION_DICT_DELETE = 2104L;
    private static final long ACTION_ONLINE_FORCE_LOGOUT = 2201L;
    private static final long ACTION_ONLINE_LOGOUT = 2202L;
    private static final long ACTION_JOB_ADD = 2301L;
    private static final long ACTION_JOB_EDIT = 2302L;
    private static final long ACTION_JOB_DELETE = 2303L;
    private static final long ACTION_JOB_STATUS = 2304L;
    private static final long ACTION_JOB_RUN = 2305L;
    private static final long ACTION_JOB_EXPORT = 2306L;
    private static final long ACTION_JOB_LOG_CLEAN = 2311L;
    private static final long ACTION_JOB_LOG_EXPORT = 2312L;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private BaseMenuMapper baseMenuMapper;
    @Autowired
    private BaseActionMapper baseActionMapper;
    @Autowired
    private BaseUserMapper baseUserMapper;
    @Autowired
    private BaseAuthorityService baseAuthorityService;
    @Autowired
    private BaseRoleService baseRoleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        long menuCount = baseMenuMapper.selectCount(null);
        log.info("补全管理后台 Vue 菜单与按钮权限元数据（当前菜单数={}），不创建测试用户", menuCount);
        seedMenusAndAuthorities();
        seedButtonActions();
        grantSuperAdminAllAuthorities();
        if (!isMarked()) {
            markDone();
            log.info("管理后台 Vue RBAC 元数据种子完成（仅超管可用，其余请管理端配置）");
        }
    }

    private boolean isMarked() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM jbm_system_init_marker WHERE marker_key = ?",
                Integer.class, MARKER_KEY);
        return count != null && count > 0;
    }

    private void markDone() {
        jdbcTemplate.update(
                "INSERT INTO jbm_system_init_marker (marker_key, initialized_at) VALUES (?, CURRENT_TIMESTAMP) "
                        + "ON DUPLICATE KEY UPDATE initialized_at = CURRENT_TIMESTAMP",
                MARKER_KEY);
    }

    private void seedMenusAndAuthorities() {
        upsertMenu(MENU_PLATFORM, null, "platform", "平台管理", "/", 0);
        upsertMenu(MENU_DASHBOARD, MENU_PLATFORM, "dashboard", "仪表盘", "/dashboard", 0);

        upsertMenu(MENU_SYSTEM, MENU_PLATFORM, "system", "系统管理", "/system", 1);
        upsertMenu(MENU_USERS, MENU_SYSTEM, "users", "用户管理", "/system/users", 1);
        upsertMenu(MENU_ONLINE_USERS, MENU_SYSTEM, "onlineUsers", "在线用户", "/system/online-users", 2);
        upsertMenu(MENU_ROLES, MENU_SYSTEM, "roles", "角色管理", "/system/roles", 3);
        upsertMenu(MENU_MENUS, MENU_SYSTEM, "menus", "菜单管理", "/system/menus", 4);
        upsertMenu(MENU_ACTIONS, MENU_SYSTEM, "actions", "按钮管理", "/system/actions", 5);
        upsertMenu(MENU_ORGS, MENU_SYSTEM, "orgs", "组织管理", "/system/orgs", 6);
        upsertMenu(MENU_AUTHORITY, MENU_SYSTEM, "authority", "权限管理", "/authority/catalog", 7);
        upsertMenu(MENU_APPS, MENU_SYSTEM, "apps", "应用管理", "/system/apps", 8);
        upsertMenu(MENU_DICTS, MENU_SYSTEM, "dicts", "字典管理", "/system/dicts", 9);
        upsertMenu(MENU_EXTEND, MENU_SYSTEM, "extend_fields", "扩展字段", "/system/extend-fields", 10);
        upsertMenu(MENU_DEVELOPER, MENU_SYSTEM, "developer_mgmt", "开发者管理", "/developer", 11);
        upsertMenu(MENU_API_KEY, MENU_SYSTEM, "api_key_mgmt", "API Key 管理", "/developer/api-keys", 12);

        upsertMenu(MENU_GATEWAY, MENU_PLATFORM, "gateway", "网关管理", "/gateway", 2);
        upsertMenu(MENU_GW_ROUTE, MENU_GATEWAY, "gw_routes", "路由管理", "/gateway/routes", 1);
        upsertMenu(MENU_GW_RATE, MENU_GATEWAY, "gw_rate", "限流管理", "/gateway/rate-limit", 2);
        upsertMenu(MENU_GW_IP, MENU_GATEWAY, "gw_ip", "IP 限制", "/gateway/ip-limit", 3);

        upsertMenu(MENU_API_MGMT, MENU_PLATFORM, "api_mgmt", "API 管理", "/api", 4);
        upsertMenu(MENU_API_REGISTRY, MENU_API_MGMT, "api_registry", "API 资源管理", "/api/registry", 1);
        upsertMenu(MENU_API_DOCS, MENU_API_MGMT, "api_docs", "API 文档与调试", "/api/docs", 2);

        upsertMenu(MENU_MESSAGES, MENU_PLATFORM, "messages", "消息管理", "/messages", 3);
        upsertMenu(MENU_MESSAGE_CENTER, MENU_MESSAGES, "message_records", "消息记录", "/messages", 1);
        upsertMenu(MENU_MESSAGE_SEND_TEST, MENU_MESSAGES, "message_send_test", "发送测试", "/messages/send-test", 2);
        upsertMenu(MENU_MESSAGE_CHANNELS, MENU_MESSAGES, "message_channels", "渠道设置", "/messages/channels", 3);

        upsertMenu(MENU_JOBS, MENU_PLATFORM, "jobs", "任务调度", "/jobs", 6);
        upsertMenu(MENU_JOB_LIST, MENU_JOBS, "task_jobs", "任务管理", "/jobs", 1);
        upsertMenu(MENU_JOB_LOGS, MENU_JOBS, "task_job_logs", "调度日志", "/jobs/logs", 2);

        upsertMenu(MENU_DOCS, MENU_PLATFORM, "documents", "文档管理", "/documents", 7);
        upsertMenu(MENU_DOC_FILES, MENU_DOCS, "doc_files", "文件管理", "/documents", 1);

        upsertMenu(MENU_OTHER, MENU_PLATFORM, "other", "其他", "/log", 5);
        upsertMenu(MENU_LOGS, MENU_OTHER, "account_logs", "审计日志", "/log/account", 1);
    }

    private void seedButtonActions() {
        seedAction(ACTION_USERS_VIEW, MENU_USERS, "users_view", "用户-查看", 1);
        seedAction(ACTION_USERS_ADD, MENU_USERS, "users_add", "用户-新增", 2);
        seedAction(ACTION_USERS_EDIT, MENU_USERS, "users_edit", "用户-编辑", 3);
        seedAction(ACTION_USERS_DELETE, MENU_USERS, "users_delete", "用户-删除", 4);
        seedAction(ACTION_DICT_VIEW, MENU_DICTS, "dict_view", "字典-查看", 1);
        seedAction(ACTION_DICT_ADD, MENU_DICTS, "dict_add", "字典-新增", 2);
        seedAction(ACTION_DICT_EDIT, MENU_DICTS, "dict_edit", "字典-编辑", 3);
        seedAction(ACTION_DICT_DELETE, MENU_DICTS, "dict_delete", "字典-删除", 4);
        seedAction(ACTION_ONLINE_FORCE_LOGOUT, MENU_ONLINE_USERS, "monitor:online:forceLogout", "在线用户-踢出", 1);
        seedAction(ACTION_ONLINE_LOGOUT, MENU_ONLINE_USERS, "monitor:online:logout", "在线用户-注销", 2);
        seedAction(ACTION_JOB_ADD, MENU_JOB_LIST, "job_add", "任务-新增", 1);
        seedAction(ACTION_JOB_EDIT, MENU_JOB_LIST, "job_edit", "任务-编辑", 2);
        seedAction(ACTION_JOB_DELETE, MENU_JOB_LIST, "job_delete", "任务-删除", 3);
        seedAction(ACTION_JOB_STATUS, MENU_JOB_LIST, "job_status", "任务-启停", 4);
        seedAction(ACTION_JOB_RUN, MENU_JOB_LIST, "job_run", "任务-执行", 5);
        seedAction(ACTION_JOB_EXPORT, MENU_JOB_LIST, "job_export", "任务-导出", 6);
        seedAction(ACTION_JOB_LOG_CLEAN, MENU_JOB_LOGS, "job_log_clean", "调度日志-清空", 1);
        seedAction(ACTION_JOB_LOG_EXPORT, MENU_JOB_LOGS, "job_log_export", "调度日志-导出", 2);
    }

    /** 仅将菜单+按钮授权给超级管理员角色，供超管在界面中为其他角色分配 */
    private void grantSuperAdminAllAuthorities() {
        List<String> ids = new ArrayList<>();
        ids.add(String.valueOf(MENU_DASHBOARD));
        ids.add(String.valueOf(MENU_USERS));
        ids.add(String.valueOf(MENU_ONLINE_USERS));
        ids.add(String.valueOf(MENU_ROLES));
        ids.add(String.valueOf(MENU_MENUS));
        ids.add(String.valueOf(MENU_ACTIONS));
        ids.add(String.valueOf(MENU_ORGS));
        ids.add(String.valueOf(MENU_AUTHORITY));
        ids.add(String.valueOf(MENU_APPS));
        ids.add(String.valueOf(MENU_DICTS));
        ids.add(String.valueOf(MENU_EXTEND));
        ids.add(String.valueOf(MENU_DEVELOPER));
        ids.add(String.valueOf(MENU_API_KEY));
        ids.add(String.valueOf(MENU_GW_ROUTE));
        ids.add(String.valueOf(MENU_GW_RATE));
        ids.add(String.valueOf(MENU_GW_IP));
        ids.add(String.valueOf(MENU_API_REGISTRY));
        ids.add(String.valueOf(MENU_API_DOCS));
        ids.add(String.valueOf(MENU_MESSAGE_CENTER));
        ids.add(String.valueOf(MENU_MESSAGE_SEND_TEST));
        ids.add(String.valueOf(MENU_MESSAGE_CHANNELS));
        ids.add(String.valueOf(MENU_JOB_LIST));
        ids.add(String.valueOf(MENU_JOB_LOGS));
        ids.add(String.valueOf(MENU_DOC_FILES));
        ids.add(String.valueOf(MENU_LOGS));
        ids.add(authorityIdForAction(ACTION_USERS_VIEW, "users_view"));
        ids.add(authorityIdForAction(ACTION_USERS_ADD, "users_add"));
        ids.add(authorityIdForAction(ACTION_USERS_EDIT, "users_edit"));
        ids.add(authorityIdForAction(ACTION_USERS_DELETE, "users_delete"));
        ids.add(authorityIdForAction(ACTION_DICT_VIEW, "dict_view"));
        ids.add(authorityIdForAction(ACTION_DICT_ADD, "dict_add"));
        ids.add(authorityIdForAction(ACTION_DICT_EDIT, "dict_edit"));
        ids.add(authorityIdForAction(ACTION_DICT_DELETE, "dict_delete"));
        ids.add(authorityIdForAction(ACTION_ONLINE_FORCE_LOGOUT, "monitor:online:forceLogout"));
        ids.add(authorityIdForAction(ACTION_ONLINE_LOGOUT, "monitor:online:logout"));
        ids.add(authorityIdForAction(ACTION_JOB_ADD, "job_add"));
        ids.add(authorityIdForAction(ACTION_JOB_EDIT, "job_edit"));
        ids.add(authorityIdForAction(ACTION_JOB_DELETE, "job_delete"));
        ids.add(authorityIdForAction(ACTION_JOB_STATUS, "job_status"));
        ids.add(authorityIdForAction(ACTION_JOB_RUN, "job_run"));
        ids.add(authorityIdForAction(ACTION_JOB_EXPORT, "job_export"));
        ids.add(authorityIdForAction(ACTION_JOB_LOG_CLEAN, "job_log_clean"));
        ids.add(authorityIdForAction(ACTION_JOB_LOG_EXPORT, "job_log_export"));

        List<OpenAuthority> current = baseAuthorityService.findAuthorityByRole(JbmConstants.ROOT_ROLE_ID);
        Set<String> merged = new LinkedHashSet<>(ids);
        if (current != null) {
            for (OpenAuthority o : current) {
                if (o.getAuthorityId() != null) {
                    merged.add(o.getAuthorityId());
                }
            }
        }
        baseAuthorityService.addAuthorityRole(
                JbmConstants.ROOT_ROLE_ID,
                null,
                merged.toArray(new String[0]));
    }

    private void upsertMenu(Long menuId, Long parentId, String code, String name, String path, int priority) {
        BaseMenu existing = baseMenuMapper.selectById(menuId);
        Date now = new Date();
        if (existing == null) {
            BaseMenu menu = new BaseMenu();
            menu.setMenuId(menuId);
            menu.setParentId(parentId);
            menu.setMenuCode(code);
            menu.setMenuName(name);
            menu.setPath(path);
            menu.setScheme("/");
            menu.setTarget("_self");
            menu.setPriority(priority);
            menu.setStatus(JbmConstants.ENABLED);
            menu.setHidden(1);
            menu.setIsPersist(true);
            menu.setServiceId("jbm-cluster-platform-center");
            menu.setCreateTime(now);
            menu.setUpdateTime(now);
            baseMenuMapper.insert(menu);
        } else {
            existing.setParentId(parentId);
            existing.setMenuCode(code);
            existing.setMenuName(name);
            existing.setPath(path);
            existing.setPriority(priority);
            existing.setStatus(JbmConstants.ENABLED);
            existing.setUpdateTime(now);
            baseMenuMapper.updateById(existing);
        }
        baseAuthorityService.saveOrUpdateAuthority(menuId, ResourceType.menu);
    }

    private void seedAction(Long actionId, Long menuId, String code, String name, int priority) {
        BaseAction existing = baseActionMapper.selectById(actionId);
        if (existing == null && StrUtil.isNotBlank(code)) {
            QueryWrapper<BaseAction> byCode = new QueryWrapper<>();
            byCode.lambda().eq(BaseAction::getActionCode, code);
            existing = baseActionMapper.selectOne(byCode);
        }
        Date now = new Date();
        if (existing == null) {
            BaseAction action = new BaseAction();
            action.setActionId(actionId);
            action.setMenuId(menuId);
            action.setActionCode(code);
            action.setActionName(name);
            action.setPriority(priority);
            action.setStatus(JbmConstants.ENABLED);
            action.setIsPersist(JbmConstants.ENABLED);
            action.setServiceId("jbm-cluster-platform-center");
            action.setCreateTime(now);
            action.setUpdateTime(now);
            baseActionMapper.insert(action);
        } else {
            existing.setMenuId(menuId);
            existing.setActionCode(code);
            existing.setActionName(name);
            existing.setPriority(priority);
            existing.setStatus(JbmConstants.ENABLED);
            existing.setUpdateTime(now);
            baseActionMapper.updateById(existing);
        }
        long effectiveActionId = existing != null ? existing.getActionId() : actionId;
        baseAuthorityService.saveOrUpdateAuthority(effectiveActionId, ResourceType.action);
    }

    private String authorityIdForAction(Long preferredActionId, String actionCode) {
        long actionId = resolveActionId(preferredActionId, actionCode);
        BaseAuthority auth = baseAuthorityService.getAuthority(actionId, ResourceType.action);
        if (auth == null || auth.getAuthorityId() == null) {
            throw new IllegalStateException("按钮权限未生成: actionId=" + actionId + " code=" + actionCode);
        }
        return String.valueOf(auth.getAuthorityId());
    }

    private long resolveActionId(Long preferredActionId, String actionCode) {
        if (baseActionMapper.selectById(preferredActionId) != null) {
            return preferredActionId;
        }
        if (StrUtil.isNotBlank(actionCode)) {
            QueryWrapper<BaseAction> byCode = new QueryWrapper<>();
            byCode.lambda().eq(BaseAction::getActionCode, actionCode);
            BaseAction found = baseActionMapper.selectOne(byCode);
            if (found != null) {
                return found.getActionId();
            }
        }
        throw new IllegalStateException("标准按钮不存在: preferredId=" + preferredActionId + " code=" + actionCode);
    }

    /** 将登录名为 admin 的用户挂上超级管理员角色（兼容非 ROOT_USER_ID 的历史库） */
    private void linkAdminUsersToSuperRole() {
        QueryWrapper<BaseUser> q = new QueryWrapper<>();
        q.lambda().eq(BaseUser::getUserName, JbmConstants.ROOT_USER_NAME);
        List<BaseUser> admins = baseUserMapper.selectList(q);
        for (BaseUser admin : admins) {
            baseRoleService.saveUserRoles(admin.getUserId(), String.valueOf(JbmConstants.ROOT_ROLE_ID));
        }
    }
}
