package com.jbm.cluster.job.scheduler;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.job.SysJob;
import com.jbm.cluster.api.entitys.job.SysJobLog;
import com.jbm.cluster.common.basic.module.JbmRequestTemplate;
import com.jbm.cluster.job.service.SysJobLogService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 单次任务执行：ShedLock（Redis）+ 本机非并发控制，替代原 {@code AbstractQuartzJob}。
 */
@Slf4j
@Component
public class JobExecutionRunner {

    @Resource
    private LockProvider lockProvider;

    @Resource
    private JbmRequestTemplate jbmRequestTemplate;

    @Resource
    private SysJobLogService sysJobLogService;

    @Value("${jbm.job.lock-at-most-for:PT30M}")
    private String lockAtMostForStr;

    @Value("${jbm.job.lock-at-least-for:PT5S}")
    private String lockAtLeastForStr;

    private Duration lockAtMostFor;
    private Duration lockAtLeastFor;

    private static final ThreadLocal<Date> THREAD_START = new ThreadLocal<>();

    private final ConcurrentHashMap<Long, ReentrantLock> localLocks = new ConcurrentHashMap<>();

    @PostConstruct
    void parseDurations() {
        this.lockAtMostFor = Duration.parse(lockAtMostForStr.trim());
        this.lockAtLeastFor = Duration.parse(lockAtLeastForStr.trim());
    }

    /**
     * 执行一次任务（含 ShedLock 与可选的本机串行）。
     */
    public void execute(SysJob sysJob) {
        if (sysJob == null || sysJob.getJobId() == null) {
            return;
        }
        String lockName = "jbm-sys-job-" + sysJob.getJobId();
        Optional<SimpleLock> lock = lockProvider.lock(
                new LockConfiguration(Instant.now(), lockName, lockAtMostFor, lockAtLeastFor));
        if (!lock.isPresent()) {
            log.debug("未获取到分布式锁，跳过: {}", lockName);
            return;
        }
        SimpleLock simpleLock = lock.get();
        try {
            executeWithLocalConcurrency(sysJob);
        } finally {
            simpleLock.unlock();
        }
    }

    private void executeWithLocalConcurrency(SysJob sysJob) {
        ReentrantLock local = null;
        if (!Boolean.TRUE.equals(sysJob.getConcurrent())) {
            local = localLocks.computeIfAbsent(sysJob.getJobId(), id -> new ReentrantLock());
            if (!local.tryLock()) {
                log.debug("本机任务仍在执行，跳过: jobId={}", sysJob.getJobId());
                return;
            }
        }
        Date executeTime = new Date();
        try {
            THREAD_START.set(executeTime);
            jbmRequestTemplate.request(sysJob.getInvokeTarget(), sysJob.getMethodType(), null);
            after(sysJob, null);
        } catch (Exception e) {
            log.error("任务执行异常: {}", sysJob.getInvokeTarget(), e);
            after(sysJob, e);
        } finally {
            THREAD_START.remove();
            if (local != null) {
                local.unlock();
            }
        }
    }

    private void after(SysJob sysJob, Exception e) {
        if (BooleanUtil.isFalse(sysJob.getRecordLog())) {
            return;
        }
        Date startTime = THREAD_START.get();
        if (startTime == null) {
            startTime = new Date();
        }
        SysJobLog sysJobLog = new SysJobLog();
        sysJobLog.setJobName(sysJob.getJobName());
        sysJobLog.setJobGroup(sysJob.getJobGroup());
        sysJobLog.setInvokeTarget(sysJob.getInvokeTarget());
        sysJobLog.setStartTime(startTime);
        sysJobLog.setStopTime(DateTime.now());
        long runMs = DateUtil.between(sysJobLog.getStopTime(), sysJobLog.getStartTime(), DateUnit.MS);
        sysJobLog.setRunTime(runMs);
        sysJobLog.setJobMessage(sysJobLog.getJobName() + "总共耗时：" + runMs + "毫秒");
        if (e != null) {
            sysJobLog.setStatus("1");
            String errorMsg = StrUtil.sub(ExceptionUtil.getMessage(e), 0, 2000);
            sysJobLog.setExceptionInfo(errorMsg);
        } else {
            sysJobLog.setStatus("0");
        }
        sysJobLogService.saveEntity(sysJobLog);
    }
}
