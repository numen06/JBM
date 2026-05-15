package com.jbm.cluster.job.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 周期性从库同步任务注册；可通过 {@code jbm.job.sync-enabled=false} 关闭（避免测试或本地进程无意义循环）。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "jbm.job", name = "sync-enabled", havingValue = "true", matchIfMissing = true)
public class JobSchedulerSync {

    private final JobSchedulerManager jobSchedulerManager;

    public JobSchedulerSync(JobSchedulerManager jobSchedulerManager) {
        this.jobSchedulerManager = jobSchedulerManager;
    }

    @Scheduled(fixedDelayString = "${jbm.job.sync-interval-ms:60000}")
    public void syncFromDatabase() {
        jobSchedulerManager.syncFromDatabase();
    }
}
