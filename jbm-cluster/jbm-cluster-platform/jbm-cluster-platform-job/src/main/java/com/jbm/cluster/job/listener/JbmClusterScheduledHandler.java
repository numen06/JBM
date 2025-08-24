package com.jbm.cluster.job.listener;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jbm.cluster.api.constants.job.MisfirePolicy;
import com.jbm.cluster.api.constants.job.ScheduleStauts;
import com.jbm.cluster.api.entitys.job.SysJob;
import com.jbm.cluster.api.model.job.JbmClusterJob;
import com.jbm.cluster.api.model.job.JbmClusterJobResource;
import com.jbm.cluster.job.service.SysJobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * MQ消息接收者 - 响应式安全版本
 *
 * @author wesley.zhang
 */
@Configuration
@Slf4j
public class JbmClusterScheduledHandler {

    private final SysJobService sysJobService;

    @Autowired
    public JbmClusterScheduledHandler(SysJobService sysJobService) {
        this.sysJobService = sysJobService;
    }

    /**
     * 接收 RabbitMQ 消息并异步处理，支持重试和 DLQ
     */
    @Bean
    public Function<Flux<Message<JbmClusterJobResource>>, Mono<Void>> scheduledJob() {
        return flux -> flux.flatMap(message -> {
            JbmClusterJobResource payload = message.getPayload();
            // 使用 flatMap 返回 Mono，确保处理完成才继续
            return Mono.fromRunnable(() -> scheduledJobQueue(payload))
                    .then(Mono.just(message)) // 成功后返回原消息（用于 ACK）
                    .onErrorMap(ex -> {
                        log.error("处理消息失败，payload: {}", payload, ex);
                        return ex; // 异常向上传播，触发 NACK 和重试
                    });
        }).then();
    }

    /**
     * 接收集群推送的定时任务资源，并同步到本地调度系统
     */
    public void scheduledJobQueue(JbmClusterJobResource jbmClusterJobResource) {
        List<JbmClusterJob> jbmClusterJobs = jbmClusterJobResource.getJbmClusterJobs();
        if (CollUtil.isEmpty(jbmClusterJobs)) {
            log.info("接收到空的定时任务列表，serviceId: {}", jbmClusterJobResource.getServiceId());
            return;
        }

        List<SysJob> serviceJobs = sysJobService.selectJobsByGroup(jbmClusterJobResource.getServiceId());
        log.info("接收到集群推送的定时任务，数量为: {}", jbmClusterJobs.size());

        for (JbmClusterJob jbmClusterJob : jbmClusterJobs) {
            if (ObjectUtil.isEmpty(jbmClusterJob)) {
                continue;
            }
            try {
                SysJob sysJob = conventType(jbmClusterJob);
                // 查找已存在的任务
                SysJob dbJob = serviceJobs.stream()
                        .filter(job -> job.getJobName().equals(jbmClusterJob.getJobName()) &&
                                job.getJobGroup().equals(jbmClusterJob.getServiceName()))
                        .findFirst()
                        .orElse(null);

                if (ObjectUtil.isNotEmpty(dbJob)) {
                    // 已存在：暂停 → 更新
                    sysJobService.pauseJob(dbJob);
                    sysJob.setJobId(dbJob.getJobId());
                    sysJobService.saveEntity(sysJob);
                    serviceJobs.remove(dbJob);
                } else {
                    // 不存在：插入新任务
                    sysJobService.insertJob(sysJob);
                }

                // 如果启用，则恢复运行
                if (jbmClusterJob.getEnable()) {
                    sysJobService.resumeJob(sysJob);
                }
            } catch (Exception e) {
                log.error("处理定时任务 [{}] 时发生错误", jbmClusterJob.getJobName(), e);
                // ❌ 不要吞掉异常！这里抛出，让上层捕获
                throw new RuntimeException("处理任务失败: " + jbmClusterJob.getJobName(), e);
            }
        }

        // 处理剩余任务（删除或暂停）
        try {
            if (CollUtil.isNotEmpty(serviceJobs)) {
                sysJobService.pauseJobs(serviceJobs);
                log.info("已暂停 {} 个被移除的任务", serviceJobs.size());
            }
        } catch (Exception e) {
            log.error("暂停被删除的任务时失败", e);
            throw new RuntimeException("暂停旧任务失败", e);
        }
    }

    /**
     * 转换 JbmClusterJob 为 SysJob
     */
    private SysJob conventType(JbmClusterJob jbmClusterEventBean) {
        SysJob sysJob = new SysJob();
        BeanUtil.copyProperties(jbmClusterEventBean, sysJob);
        sysJob.setCreateBy("SYSTEM");
        sysJob.setMethodType(jbmClusterEventBean.getMethodType());
        sysJob.setMisfirePolicy(MisfirePolicy.DO_NOTHING);
        sysJob.setCronExpression(jbmClusterEventBean.getCron());
        sysJob.setConcurrent(false);
        sysJob.setStatus(ScheduleStauts.PAUSE);
        sysJob.setJobName(jbmClusterEventBean.getJobName());
        sysJob.setJobGroup(jbmClusterEventBean.getServiceName());
        sysJob.setInvokeTarget(jbmClusterEventBean.getUrl());
        return sysJob;
    }
}