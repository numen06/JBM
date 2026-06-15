package com.jbm.cluster.common.mysql.init;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 首次启动补全<strong>标准菜单 + 标准按钮</strong>（与 Vue 路由、页内 ACTION_* 对齐），并仅授权给超管角色。
 * <p>
 * 种子数据见 {@code classpath:data/admin-vue-rbac-seed.json}，新增菜单/按钮只需改该文件并重启 center。
 * 之后由超级管理员在「菜单管理」「按钮管理」「角色管理」中增删改；本类不会覆盖已存在数据。
 * 不创建业务用户/业务角色（见 {@link SystemDataInitializer} 仅初始化超管）。
 */
@Slf4j
@Component
@Order(Integer.MAX_VALUE - 90)
@ConditionalOnProperty(name = "jbm.cluster.data-init.vue-rbac-metadata-enabled", havingValue = "true", matchIfMissing = true)
public class AdminVueRbacSeedInitializer implements ApplicationRunner {

    private static final String MARKER_KEY = "admin_vue_rbac_v1";
    private static final String SEED_RESOURCE = "/data/admin-vue-rbac-seed.json";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

    private AdminVueRbacSeedCatalog catalog;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        catalog = loadCatalog();
        long menuCount = baseMenuMapper.selectCount(null);
        log.info("补全管理后台 Vue 菜单与按钮权限元数据（当前菜单数={}，种子文件={}）", menuCount, SEED_RESOURCE);
        seedMenusAndAuthorities();
        seedButtonActions();
        grantSuperAdminAllAuthorities();
        if (!isMarked()) {
            markDone();
            log.info("管理后台 Vue RBAC 元数据种子完成（仅超管可用，其余请管理端配置）");
        }
    }

    private AdminVueRbacSeedCatalog loadCatalog() {
        try (InputStream in = AdminVueRbacSeedInitializer.class.getResourceAsStream(SEED_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("RBAC seed file not found: " + SEED_RESOURCE);
            }
            AdminVueRbacSeedCatalog loaded = OBJECT_MAPPER.readValue(in, AdminVueRbacSeedCatalog.class);
            if (loaded.getMenus().isEmpty()) {
                throw new IllegalStateException("RBAC seed menus must not be empty: " + SEED_RESOURCE);
            }
            return loaded;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load RBAC seed file: " + SEED_RESOURCE, e);
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
        for (AdminVueRbacSeedCatalog.MenuSeed menu : catalog.getMenus()) {
            upsertMenu(menu.getId(), menu.getParentId(), menu.getCode(), menu.getName(), menu.getPath(), menu.getPriority());
        }
    }

    private void seedButtonActions() {
        for (AdminVueRbacSeedCatalog.ActionSeed action : catalog.getActions()) {
            seedAction(action.getId(), action.getMenuId(), action.getCode(), action.getName(), action.getPriority());
        }
    }

    /** 将种子文件中标记 grantToSuperAdmin 的菜单/按钮授权给超级管理员角色 */
    private void grantSuperAdminAllAuthorities() {
        List<String> ids = new ArrayList<>();
        for (AdminVueRbacSeedCatalog.MenuSeed menu : catalog.getMenus()) {
            if (menu.isGrantToSuperAdmin()) {
                ids.add(String.valueOf(menu.getId()));
            }
        }
        for (AdminVueRbacSeedCatalog.ActionSeed action : catalog.getActions()) {
            if (action.isGrantToSuperAdmin()) {
                ids.add(authorityIdForAction(action.getId(), action.getCode()));
            }
        }

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
