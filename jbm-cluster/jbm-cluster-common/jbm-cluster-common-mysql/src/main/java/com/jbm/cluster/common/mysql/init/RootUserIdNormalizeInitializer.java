package com.jbm.cluster.common.mysql.init;

import com.jbm.cluster.core.constant.JbmConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * jaja7 老库兼容：将历史雪花 ID 的 admin 账号归一到固定 ROOT_USER_ID=1。
 */
@Slf4j
@Component
@Order(Integer.MAX_VALUE - 120)
@ConditionalOnProperty(name = "jbm.cluster.data-init.force-reset-default-password", havingValue = "true")
public class RootUserIdNormalizeInitializer implements ApplicationRunner {

    private static final String MARKER_KEY = "root_user_id_normalized_v1";

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        Long adminId = queryLong("SELECT user_id FROM base_user WHERE user_name = ? LIMIT 1",
                JbmConstants.ROOT_USER_NAME);
        if (adminId == null) {
            log.info("root-user-id-normalize: 无 admin 用户，跳过");
            return;
        }
        if (JbmConstants.ROOT_USER_ID.equals(adminId)) {
            enforceRootUserShape();
            markInitialized();
            return;
        }
        Long rootSlot = queryLong("SELECT user_id FROM base_user WHERE user_id = ? LIMIT 1",
                JbmConstants.ROOT_USER_ID);
        if (rootSlot != null) {
            log.warn("root-user-id-normalize: user_id=1 已存在，无法将 admin({}) 归一，跳过", adminId);
            return;
        }

        normalizeReferences(adminId);
        normalizeAdminAccount(adminId);
        int users = jdbcTemplate.update(
                "UPDATE base_user SET user_id = ?, user_type = ?, real_name = COALESCE(real_name, nick_name, ?), update_time = CURRENT_TIMESTAMP WHERE user_id = ?",
                JbmConstants.ROOT_USER_ID, JbmConstants.USER_TYPE_SUPER, "超级管理员", adminId);
        markInitialized();
        log.info("root-user-id-normalize: admin user_id {} -> {}, users={}", adminId, JbmConstants.ROOT_USER_ID, users);
    }

    private void normalizeReferences(Long adminId) {
        updateColumn("base_account", "user_id", adminId);
        updateColumn("base_account_logs", "user_id", adminId);
        updateColumn("base_authority_user", "user_id", adminId);
        updateColumn("base_role_user", "user_id", adminId);
        updateColumn("published_api_doc", "publisher_user_id", adminId);
        updateColumn("push_message_body", "send_user_id", adminId);
        updateColumn("push_message_item", "rec_user_id", adminId);
        updateColumn("push_message_item", "send_user_id", adminId);
    }

    private void updateColumn(String table, String column, Long adminId) {
        try {
            int count = jdbcTemplate.update("UPDATE " + table + " SET " + column + " = ? WHERE " + column + " = ?",
                    JbmConstants.ROOT_USER_ID, adminId);
            if (count > 0) {
                log.info("root-user-id-normalize: {}.{} updated {}", table, column, count);
            }
        } catch (Exception e) {
            log.warn("root-user-id-normalize: 更新 {}.{} 失败: {}", table, column, e.getMessage());
        }
    }

    private void normalizeAdminAccount(Long adminId) {
        Long adminAccountId = queryLong("SELECT account_id FROM base_account WHERE account = ? AND user_id = ? LIMIT 1",
                JbmConstants.ROOT_USER_NAME, JbmConstants.ROOT_USER_ID);
        if (adminAccountId == null) {
            adminAccountId = queryLong("SELECT account_id FROM base_account WHERE account = ? AND user_id = ? LIMIT 1",
                    JbmConstants.ROOT_USER_NAME, adminId);
        }
        if (adminAccountId == null || JbmConstants.ROOT_USER_ID.equals(adminAccountId)) {
            return;
        }
        Long rootAccount = queryLong("SELECT account_id FROM base_account WHERE account_id = ? LIMIT 1",
                JbmConstants.ROOT_USER_ID);
        if (rootAccount == null) {
            jdbcTemplate.update("UPDATE base_account SET account_id = ? WHERE account_id = ?",
                    JbmConstants.ROOT_USER_ID, adminAccountId);
            log.info("root-user-id-normalize: admin account_id {} -> {}", adminAccountId, JbmConstants.ROOT_USER_ID);
        }
    }

    private void enforceRootUserShape() {
        jdbcTemplate.update(
                "UPDATE base_user SET user_type = ?, real_name = COALESCE(real_name, nick_name, ?), update_time = CURRENT_TIMESTAMP WHERE user_id = ? AND user_name = ?",
                JbmConstants.USER_TYPE_SUPER, "超级管理员", JbmConstants.ROOT_USER_ID, JbmConstants.ROOT_USER_NAME);
        jdbcTemplate.update("UPDATE base_account SET user_id = ? WHERE account = ?",
                JbmConstants.ROOT_USER_ID, JbmConstants.ROOT_USER_NAME);
    }

    private void markInitialized() {
        try {
            jdbcTemplate.update(
                    "INSERT INTO jbm_system_init_marker (marker_key, initialized_at) VALUES (?, CURRENT_TIMESTAMP) "
                            + "ON DUPLICATE KEY UPDATE initialized_at = VALUES(initialized_at)",
                    MARKER_KEY);
        } catch (Exception e) {
            log.warn("root-user-id-normalize: 写入 marker 失败: {}", e.getMessage());
        }
    }

    private Long queryLong(String sql, Object... args) {
        try {
            return jdbcTemplate.queryForObject(sql, Long.class, args);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}
