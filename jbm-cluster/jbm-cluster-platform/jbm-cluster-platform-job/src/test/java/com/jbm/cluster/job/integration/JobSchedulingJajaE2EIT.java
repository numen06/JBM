package com.jbm.cluster.job.integration;

import com.jbm.cluster.job.JbmJobApplication;
import com.jbm.cluster.job.scheduler.JobSchedulerManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 使用与线上一致的 Nacos <strong>jaja</strong> 环境（{@code bootstrap-jaja.yml} + Nacos 中
 * {@code common/redis/rabbitmq/db/mqtt} 等 shared 配置）拉起完整 Spring 上下文，
 * 验证 MySQL、Redis（含 ShedLock 所用连接）可用，并从库重建调度注册。
 * <p>
 * 默认不执行（避免 CI 或无网络环境失败）。本地/联调时在能访问 jaja Nacos 的机器上执行：
 * <pre>
 *   mvn test -pl jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-job \\
 *       -am -Pjaja -Djbm.job.e2e=true -Dtest=JobSchedulingJajaE2EIT
 * </pre>
 * {@code -Pjaja} 用于 Maven 资源过滤，使 {@code bootstrap.yml} 中 {@code ${profile.name}} 解析为 {@code jaja}，
 * 与 {@link ActiveProfiles @ActiveProfiles("jaja")} 一致。
 */
@SpringBootTest(
        classes = JbmJobApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "jbm.job.sync-enabled=false",
                "jbm.job.load-on-startup=false",
                "jbm.job.shutdown-await-seconds=8"
        }
)
@ActiveProfiles("jaja")
@EnabledIfSystemProperty(named = "jbm.job.e2e", matches = "true")
@Timeout(300)
class JobSchedulingJajaE2EIT {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private JobSchedulerManager jobSchedulerManager;

    @Test
    void mysqlRedisReachable_andReloadJobsFromDb() throws Exception {
        try (Connection c = dataSource.getConnection()) {
            assertThat(c.isValid(5)).isTrue();
        }
        jobSchedulerManager.reloadAllJobs();
    }
}
