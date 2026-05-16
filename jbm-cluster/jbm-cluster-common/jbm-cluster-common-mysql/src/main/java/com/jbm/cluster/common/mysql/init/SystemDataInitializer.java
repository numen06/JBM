package com.jbm.cluster.common.mysql.init;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.constants.ResourceType;
import com.jbm.cluster.api.entitys.basic.*;
import com.jbm.cluster.common.mysql.mapper.*;
import com.jbm.cluster.common.mysql.service.BaseAuthorityService;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 空库兜底：超级管理员、基础角色、平台菜单与权限、示例开发者应用。
 */
@Slf4j
@Component
@Order(Integer.MAX_VALUE - 100)
@ConditionalOnProperty(name = "jbm.cluster.data-init.enabled", havingValue = "true", matchIfMissing = true)
public class SystemDataInitializer implements ApplicationRunner {

    private static final String MARKER_KEY = "rbac_seed_v1";
    private static final long MENU_PLATFORM_ROOT = 100L;
    private static final long MENU_SYSTEM = 101L;
    private static final long MENU_USER = 102L;
    private static final long MENU_ROLE = 103L;
    private static final long MENU_AUTHORITY = 104L;
    private static final long MENU_DEVELOPER = 105L;
    private static final long DEV_USER_ID = 10L;
    private static final long DEV_APP_ID = 1000L;

    @Value("${jbm.cluster.data-init.root-password:admin123}")
    private String rootPassword;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private BaseUserMapper baseUserMapper;
    @Autowired
    private BaseAccountMapper baseAccountMapper;
    @Autowired
    private BaseRoleMapper baseRoleMapper;
    @Autowired
    private BaseRoleUserMapper baseRoleUserMapper;
    @Autowired
    private BaseMenuMapper baseMenuMapper;
    @Autowired
    private BaseDeveloperMapper baseDeveloperMapper;
    @Autowired
    private BaseAppMapper baseAppMapper;
    @Autowired
    private BaseAuthorityService baseAuthorityService;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        if (isMarked()) {
            return;
        }
        if (existsRootUser()) {
            markInitialized();
            return;
        }
        log.info("开始初始化系统基础数据（ROOT、角色、菜单、开发者应用）");
        seedRootUser();
        seedSuperAdminRole();
        seedPlatformMenus();
        seedDeveloperApp();
        markInitialized();
        log.info("系统基础数据初始化完成");
    }

    private boolean isMarked() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM jbm_system_init_marker WHERE marker_key = ?",
                Integer.class, MARKER_KEY);
        return count != null && count > 0;
    }

    private void markInitialized() {
        jdbcTemplate.update(
                "MERGE INTO jbm_system_init_marker (marker_key, initialized_at) KEY(marker_key) VALUES (?, CURRENT_TIMESTAMP)",
                MARKER_KEY);
    }

    private boolean existsRootUser() {
        QueryWrapper<BaseUser> q = new QueryWrapper<>();
        q.lambda().eq(BaseUser::getUserName, JbmConstants.ROOT_USER_NAME);
        return baseUserMapper.selectCount(q) > 0;
    }

    private void seedRootUser() {
        Date now = new Date();
        BaseUser user = new BaseUser();
        user.setUserId(JbmConstants.ROOT_USER_ID);
        user.setUserName(JbmConstants.ROOT_USER_NAME);
        user.setUserType(JbmConstants.USER_TYPE_SUPER);
        user.setNickName("超级管理员");
        user.setRealName("超级管理员");
        user.setStatus(JbmConstants.ENABLED);
        user.setCreateTime(now);
        user.setUpdateTime(now);
        baseUserMapper.insert(user);

        BaseAccount account = new BaseAccount(
                JbmConstants.ROOT_USER_ID,
                JbmConstants.ROOT_USER_NAME,
                SecurityUtils.encryptPassword(rootPassword),
                "password",
                "default",
                "127.0.0.1");
        account.setAccountId(JbmConstants.ROOT_USER_ID);
        account.setStatus(JbmConstants.ENABLED);
        account.setCreateTime(now);
        account.setUpdateTime(now);
        baseAccountMapper.insert(account);
    }

    private void seedSuperAdminRole() {
        Date now = new Date();
        BaseRole role = new BaseRole();
        role.setRoleId(JbmConstants.ROOT_ROLE_ID);
        role.setRoleCode("super_admin");
        role.setRoleName("超级管理员");
        role.setRoleDesc("系统内置超级管理员角色");
        role.setStatus(JbmConstants.ENABLED);
        role.setIsPersist(JbmConstants.ENABLED);
        role.setCreateTime(now);
        role.setUpdateTime(now);
        baseRoleMapper.insert(role);

        BaseRoleUser roleUser = new BaseRoleUser();
        roleUser.setId(1L);
        roleUser.setUserId(JbmConstants.ROOT_USER_ID);
        roleUser.setRoleId(JbmConstants.ROOT_ROLE_ID);
        roleUser.setCreateTime(now);
        roleUser.setUpdateTime(now);
        baseRoleUserMapper.insert(roleUser);
    }

    private void seedPlatformMenus() {
        saveMenu(MENU_PLATFORM_ROOT, null, "platform", "平台管理", "/", 0);
        saveMenu(MENU_SYSTEM, MENU_PLATFORM_ROOT, "system", "系统管理", "/system", 1);
        saveMenu(MENU_USER, MENU_SYSTEM, "user", "用户管理", "/system/user", 1);
        saveMenu(MENU_ROLE, MENU_SYSTEM, "role", "角色管理", "/system/role", 2);
        saveMenu(MENU_AUTHORITY, MENU_SYSTEM, "authority", "权限管理", "/system/authority", 3);
        saveMenu(MENU_DEVELOPER, MENU_SYSTEM, "developer", "开发者管理", "/system/developer", 4);
    }

    private void saveMenu(Long menuId, Long parentId, String code, String name, String path, int priority) {
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
        Date now = new Date();
        menu.setCreateTime(now);
        menu.setUpdateTime(now);
        baseMenuMapper.insert(menu);
        baseAuthorityService.saveOrUpdateAuthority(menuId, ResourceType.menu);
    }

    private void seedDeveloperApp() {
        Date now = new Date();
        BaseDeveloper developer = new BaseDeveloper();
        developer.setUserId(DEV_USER_ID);
        developer.setUserName("seed_developer");
        developer.setUserType("dev");
        developer.setNickName("种子开发者");
        developer.setStatus(JbmConstants.ENABLED);
        developer.setCreateTime(now);
        developer.setUpdateTime(now);
        baseDeveloperMapper.insert(developer);

        BaseApp app = new BaseApp();
        app.setAppId(DEV_APP_ID);
        app.setApiKey(JbmConstants.SEED_DEV_APP_API_KEY);
        app.setSecretKey(SecurityUtils.encryptPassword(JbmConstants.SEED_DEV_APP_SECRET));
        app.setAppType("server");
        app.setAppName("H2种子测试应用");
        app.setDeveloperId(DEV_USER_ID);
        app.setStatus(JbmConstants.ENABLED);
        app.setIsPersist(0);
        app.setCreateTime(now);
        app.setUpdateTime(now);
        baseAppMapper.insert(app);
    }
}
