package com.jbm.cluster.job.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.constants.job.ScheduleStauts;
import com.jbm.cluster.api.entitys.job.SysJob;
import com.jbm.cluster.job.mapper.SysJobMapper;
import com.jbm.cluster.job.util.CronUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 使用 {@link ScheduledThreadPoolExecutor} 按 Cron 调度任务，多实例下由 {@link JobExecutionRunner} 的 ShedLock 保证互斥。
 */
@Slf4j
@Component
public class JobSchedulerManager implements DisposableBean {

    private final SysJobMapper sysJobMapper;
    private final JobExecutionRunner jobExecutionRunner;

    @Value("${jbm.job.thread-pool-size:20}")
    private int threadPoolSize;

    @Value("${jbm.job.shutdown-await-seconds:10}")
    private int shutdownAwaitSeconds;

    public JobSchedulerManager(SysJobMapper sysJobMapper, JobExecutionRunner jobExecutionRunner) {
        this.sysJobMapper = sysJobMapper;
        this.jobExecutionRunner = jobExecutionRunner;
    }

    private ScheduledThreadPoolExecutor executor;

    private final Object gate = new Object();

    private final ConcurrentHashMap<Long, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> signatures = new ConcurrentHashMap<>();

    @PostConstruct
    public void initExecutor() {
        int n = Math.max(1, threadPoolSize);
        executor = new ScheduledThreadPoolExecutor(n, r -> {
            Thread t = new Thread(r, "jbm-job-scheduler");
            t.setDaemon(true);
            return t;
        });
        executor.setRemoveOnCancelPolicy(true);
    }

    @Override
    public void destroy() {
        if (executor != null) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(Math.max(1, shutdownAwaitSeconds), TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 启动时或全量重建：取消全部并按库中状态重建调度链。
     */
    public void reloadAllJobs() {
        synchronized (gate) {
            cancelAllFutures();
            signatures.clear();
            List<SysJob> list = sysJobMapper.selectList(new QueryWrapper<>());
            if (list == null) {
                return;
            }
            for (SysJob job : list) {
                refreshRegistrationLocked(job);
            }
        }
    }

    /**
     * 单条任务与线程池同步（供 API 修改、MQ 同步等调用）。
     */
    public void refreshJob(SysJob job) {
        if (job == null || job.getJobId() == null) {
            return;
        }
        synchronized (gate) {
            SysJob latest = sysJobMapper.selectById(job.getJobId());
            if (latest == null) {
                cancelFuture(job.getJobId());
                signatures.remove(job.getJobId());
                return;
            }
            refreshRegistrationLocked(latest);
        }
    }

    public void pauseJob(Long jobId) {
        synchronized (gate) {
            cancelFuture(jobId);
            SysJob j = sysJobMapper.selectById(jobId);
            if (j != null) {
                signatures.put(jobId, signatureOf(j));
            }
        }
    }

    public void resumeJob(Long jobId) {
        synchronized (gate) {
            cancelFuture(jobId);
            SysJob j = sysJobMapper.selectById(jobId);
            if (j == null) {
                return;
            }
            refreshRegistrationLocked(j);
        }
    }

    public void unregisterJob(Long jobId) {
        synchronized (gate) {
            cancelFuture(jobId);
            signatures.remove(jobId);
        }
    }

    /**
     * 立即执行一次（不改变库中状态逻辑由上层处理）。
     */
    public void runOnce(SysJob job) {
        if (job == null) {
            return;
        }
        jobExecutionRunner.execute(job);
    }

    /**
     * 从库同步任务注册（由 {@link JobSchedulerSync} 定时触发，也可单测/运维手动调用）。
     */
    public void syncFromDatabase() {
        try {
            synchronized (gate) {
                List<SysJob> all = sysJobMapper.selectList(new QueryWrapper<>());
                Set<Long> alive = new HashSet<>();
                if (all != null) {
                    for (SysJob j : all) {
                        alive.add(j.getJobId());
                        refreshRegistrationLocked(j);
                    }
                }
                for (Long id : new HashSet<>(futures.keySet())) {
                    if (!alive.contains(id)) {
                        cancelFuture(id);
                        signatures.remove(id);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("定时同步任务调度失败", e);
        }
    }

    private void refreshRegistrationLocked(SysJob job) {
        Long jobId = job.getJobId();
        String sig = signatureOf(job);
        if (job.getStatus() == ScheduleStauts.PAUSE) {
            cancelFuture(jobId);
            signatures.put(jobId, sig);
            return;
        }
        if (job.getStatus() != ScheduleStauts.NORMAL) {
            cancelFuture(jobId);
            signatures.put(jobId, sig);
            return;
        }
        if (!CronUtils.isValid(job.getCronExpression())) {
            log.warn("任务 cron 无效，跳过调度 jobId={} cron={}", jobId, job.getCronExpression());
            cancelFuture(jobId);
            signatures.put(jobId, sig);
            return;
        }
        String prev = signatures.get(jobId);
        ScheduledFuture<?> existing = futures.get(jobId);
        if (sig.equals(prev) && existing != null && !existing.isDone() && !existing.isCancelled()) {
            return;
        }
        cancelFuture(jobId);
        signatures.put(jobId, sig);
        scheduleNextFire(jobId, Instant.now());
    }

    private void scheduleNextFire(Long jobId, Instant baseForNext) {
        SysJob job = sysJobMapper.selectById(jobId);
        if (job == null) {
            futures.remove(jobId);
            signatures.remove(jobId);
            return;
        }
        if (job.getStatus() != ScheduleStauts.NORMAL) {
            futures.remove(jobId);
            return;
        }
        if (!CronUtils.isValid(job.getCronExpression())) {
            return;
        }
        Instant next;
        try {
            next = CronUtils.nextInstant(job.getCronExpression(), baseForNext);
        } catch (Exception e) {
            log.warn("计算下次执行时间失败 jobId={}", jobId, e);
            return;
        }
        long delayMs = Math.max(1L, Duration.between(Instant.now(), next).toMillis());
        ScheduledFuture<?> f = executor.schedule(() -> onFire(jobId), delayMs, TimeUnit.MILLISECONDS);
        futures.put(jobId, f);
    }

    private void onFire(Long jobId) {
        SysJob job = sysJobMapper.selectById(jobId);
        if (job == null) {
            futures.remove(jobId);
            signatures.remove(jobId);
            return;
        }
        if (job.getStatus() != ScheduleStauts.NORMAL) {
            futures.remove(jobId);
            return;
        }
        try {
            jobExecutionRunner.execute(job);
        } catch (Exception e) {
            log.error("任务触发执行失败 jobId={}", jobId, e);
        } finally {
            SysJob latest = sysJobMapper.selectById(jobId);
            if (latest != null && latest.getStatus() == ScheduleStauts.NORMAL) {
                scheduleNextFire(jobId, Instant.now());
            } else {
                futures.remove(jobId);
            }
        }
    }

    private static String signatureOf(SysJob j) {
        return String.valueOf(j.getCronExpression()) + "|" + j.getStatus()
                + "|" + String.valueOf(j.getInvokeTarget()) + "|" + String.valueOf(j.getMethodType())
                + "|" + String.valueOf(j.getConcurrent());
    }

    private void cancelFuture(Long jobId) {
        ScheduledFuture<?> f = futures.remove(jobId);
        if (f != null) {
            f.cancel(false);
        }
    }

    private void cancelAllFutures() {
        for (Long id : new HashSet<>(futures.keySet())) {
            cancelFuture(id);
        }
    }
}
