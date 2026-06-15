package com.jbm.cluster.common.mysql.integration;

import com.jbm.cluster.api.constants.OAuthClientSecretVerifier;
import com.jbm.cluster.common.mysql.mapper.BaseAppMapper;
import com.jbm.cluster.common.mysql.service.impl.OAuthClientSecretVerifierImpl;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import com.jbm.cluster.core.constant.JbmConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API Key 密钥校验（BCrypt 存储）。
 */
@SpringBootTest(
        classes = OAuthClientSecretVerifierH2IT.TestApp.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:jbm_oauth_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.hikari.connection-init-sql=",
                "spring.liquibase.change-log=classpath:db/cluster-rbac/db.changelog-master.yaml",
                "jbm.cluster.data-init.enabled=false"
        }
)
class OAuthClientSecretVerifierH2IT {

    @SpringBootApplication
    @MapperScan("com.jbm.cluster.common.mysql.mapper")
    @Import(OAuthClientSecretVerifierImpl.class)
    static class TestApp {
    }

    @Autowired
    private OAuthClientSecretVerifier verifier;

    @Autowired
    private BaseAppMapper baseAppMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void insertSeedApp() {
        jdbcTemplate.execute("ALTER TABLE base_app ADD COLUMN IF NOT EXISTS extend_data CLOB");
        jdbcTemplate.update("DELETE FROM base_app WHERE app_id = ?", JbmConstants.SEED_DEV_APP_API_KEY.hashCode() & Long.MAX_VALUE);
        com.jbm.cluster.api.entitys.basic.BaseApp app = new com.jbm.cluster.api.entitys.basic.BaseApp();
        app.setAppId(1000L);
        app.setApiKey(JbmConstants.SEED_DEV_APP_API_KEY);
        app.setSecretKey(SecurityUtils.encryptPassword(JbmConstants.SEED_DEV_APP_SECRET));
        app.setAppName("test");
        app.setStatus(1);
        Date now = new Date();
        app.setCreateTime(now);
        app.setUpdateTime(now);
        baseAppMapper.insert(app);
    }

    @Test
    void bcryptSecret_matchesPlainRequest() {
        assertThat(verifier.verify(JbmConstants.SEED_DEV_APP_API_KEY, JbmConstants.SEED_DEV_APP_SECRET))
                .isTrue();
        assertThat(verifier.verify(JbmConstants.SEED_DEV_APP_API_KEY, "wrong-secret"))
                .isFalse();
        assertThat(verifier.verify("fake-unregistered-key", JbmConstants.SEED_DEV_APP_SECRET))
                .isFalse();
    }
}
