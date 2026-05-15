package com.jbm.cluster.job.scheduler;

import com.jbm.cluster.api.entitys.job.SysJob;
import com.jbm.cluster.common.basic.module.JbmRequestTemplate;
import com.jbm.cluster.job.service.SysJobLogService;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ShedLock 未抢到锁时不执行业务；抢到锁时调用 {@link JbmRequestTemplate}。
 */
@ExtendWith(MockitoExtension.class)
@Timeout(20)
class JobExecutionRunnerTest {

    @Mock
    private LockProvider lockProvider;

    @Mock
    private JbmRequestTemplate jbmRequestTemplate;

    @Mock
    private SysJobLogService sysJobLogService;

    @Mock
    private SimpleLock simpleLock;

    private JobExecutionRunner runner;

    @BeforeEach
    void setUp() {
        runner = new JobExecutionRunner();
        ReflectionTestUtils.setField(runner, "lockProvider", lockProvider);
        ReflectionTestUtils.setField(runner, "jbmRequestTemplate", jbmRequestTemplate);
        ReflectionTestUtils.setField(runner, "sysJobLogService", sysJobLogService);
        ReflectionTestUtils.setField(runner, "lockAtMostFor", Duration.ofMinutes(30));
        ReflectionTestUtils.setField(runner, "lockAtLeastFor", Duration.ofSeconds(5));
    }

    @Test
    void execute_whenLockNotAcquired_doesNotInvokeTarget() throws Exception {
        when(lockProvider.lock(any(LockConfiguration.class))).thenReturn(Optional.empty());

        SysJob job = new SysJob();
        job.setJobId(1L);
        job.setInvokeTarget("feign://demo/health");
        job.setMethodType("GET");
        job.setConcurrent(true);
        job.setRecordLog(false);

        runner.execute(job);

        verify(jbmRequestTemplate, never()).request(any(), any(), any());
    }

    @Test
    void execute_whenLockAcquired_invokesTargetAndReleasesLock() throws Exception {
        when(lockProvider.lock(any(LockConfiguration.class))).thenReturn(Optional.of(simpleLock));

        SysJob job = new SysJob();
        job.setJobId(2L);
        job.setInvokeTarget("feign://demo/health");
        job.setMethodType("GET");
        job.setConcurrent(true);
        job.setRecordLog(false);

        runner.execute(job);

        verify(jbmRequestTemplate).request("feign://demo/health", "GET", null);
        verify(simpleLock).unlock();
    }
}
