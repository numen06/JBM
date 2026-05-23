package com.jbm.cluster.common.mysql.init;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 启动参数 {@code jbm.cluster.data-init.force-reset-default-password=true} 时，
 * 将超级管理员密码恢复为默认并标记须改密；同步恢复 JBM 基础应用 client 密钥。
 */
@Slf4j
@Component
@Order(Integer.MAX_VALUE - 110)
@ConditionalOnProperty(name = "jbm.cluster.data-init.force-reset-default-password", havingValue = "true")
public class RootPasswordResetInitializer implements ApplicationRunner {

    @Autowired
    private JbmJaja7SeedResetService jbmJaja7SeedResetService;

    @Override
    public void run(ApplicationArguments args) {
        Map<String, Object> result = jbmJaja7SeedResetService.resetAll();
        if (Boolean.TRUE.equals(result.get("skipped"))) {
            log.info("force-reset-default-password: 无 admin 用户，跳过");
            return;
        }
        log.info("force-reset-default-password: 完成 {}", result);
    }
}
