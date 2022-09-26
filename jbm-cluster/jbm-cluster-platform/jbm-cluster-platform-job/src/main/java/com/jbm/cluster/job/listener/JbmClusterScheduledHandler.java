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
 * mq消息接收者
 *
 * @author wesley.zhang
 */
@Configuration
@Slf4j
public class JbmClusterScheduledHandler {
    @Autowired
    private SysJobService sysJobService;


    @Bean
    public Function<Flux<Message<JbmClusterJobResource>>, Mono<Void>> scheduledJob() {
        return flux -> flux.map(message -> {
            this.scheduledJobQueue(message.getPayload());
            return message;
        }).then();
    }

    /**
     * 接收API资源扫描消息
     */
    public void scheduledJobQueue(JbmClusterJobResource jbmClusterJobResource) {
        List<JbmClusterJob> jbmClusterJobs = jbmClusterJobResource.getJbmClusterJobs();
        List<SysJob> serviceJobs = sysJobService.selectJobsByGroup(jbmClusterJobResource.getServiceId());
        log.info("接受到集群推送的定时任务,数量为:{}", CollUtil.size(jbmClusterJobs));
        for (JbmClusterJob jbmClusterJob : jbmClusterJobs) {
            if (ObjectUtil.isEmpty(jbmClusterJob)) {
                continue;
            }
            try {
                SysJob sysJob = this.conventType(jbmClusterJob);
                SysJob dbJob = CollUtil.getFirst(serviceJobs.stream().filter(job -> job.getJobName().equals(jbmClusterJob.getJobName()) && job.getJobGroup().equals(jbmClusterJob.getServiceName())).collect(Collectors.toList()));
//                SysJob dbJob = sysJobService.selectJobByName(jbmClusterJob.getJobName(), jbmClusterJob.getServiceName());
                //如果已经存在则定制运行后更新内容
                if (ObjectUtil.isNotEmpty(dbJob)) {
                    sysJobService.pauseJob(dbJob);
                    sysJob.setJobId(dbJob.getJobId());
                    sysJobService.saveEntity(sysJob);
                    serviceJobs.remove(dbJob);
                } else {
                    sysJobService.insertJob(sysJob);
                }
                if (jbmClusterJob.getEnable()) {
                    sysJobService.resumeJob(sysJob);
                }
            } catch (Exception e) {
                log.warn("添加定时任务错误", e);
            }
        }
        try {
            sysJobService.pauseJobs(serviceJobs);
        } catch (Exception e) {
            log.error("暂定删除的任务失败");
        }
    }

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
