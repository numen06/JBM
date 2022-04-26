package com.jbm.cluster.job.listener;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jbm.cluster.api.constants.job.MisfirePolicy;
import com.jbm.cluster.api.constants.job.ScheduleStauts;
import com.jbm.cluster.api.entitys.job.SysJob;
import com.jbm.cluster.common.core.bus.JbmClusterEventBean;
import com.jbm.cluster.core.constant.QueueConstants;
import com.jbm.cluster.job.service.SysJobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.Payload;

import java.util.List;

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

    /**
     * 接收API资源扫描消息
     */
    @RabbitListener(queues = QueueConstants.QUEUE_SCAN_SCHEDULED)
    public void ScanDicResourceQueue(@Payload List<JbmClusterEventBean> jbmClusterEventBeans) {
        log.info("接受到集群推送的定时任务,数量为:{}", CollUtil.size(jbmClusterEventBeans));
        for (JbmClusterEventBean jbmClusterEventBean : jbmClusterEventBeans) {
            if (ObjectUtil.isEmpty(jbmClusterEventBean))
                continue;
            try {
                sysJobService.saveEntity(this.conventType(jbmClusterEventBean));
            } catch (Exception e) {
                log.warn("添加定时任务错误", e);
            }
        }
    }

    private SysJob conventType(JbmClusterEventBean jbmClusterEventBean) {
        SysJob sysJob = new SysJob();
        BeanUtil.copyProperties(jbmClusterEventBean, sysJob);
        sysJob.setCreateBy("cluster");
        sysJob.setMisfirePolicy(MisfirePolicy.DEFAULT);
        sysJob.setCronExpression(jbmClusterEventBean.getCron());
        sysJob.setConcurrent(false);
        sysJob.setStatus(ScheduleStauts.NORMAL);
        sysJob.setJobName(jbmClusterEventBean.getJobName());
        sysJob.setJobGroup(jbmClusterEventBean.getServiceName());
        sysJob.setInvokeTarget(jbmClusterEventBean.getUrl());
        return sysJob;
    }


}
