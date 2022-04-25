package com.jbm.cluster.node.configuration.cluster;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jbm.autoconfig.dic.DictionaryTemplate;
import com.jbm.cluster.common.bus.JbmClusterEventBean;
import com.jbm.cluster.common.constants.QueueConstants;
import com.jbm.framework.dictionary.JbmDictionary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ClusterDicScan implements ApplicationListener<ApplicationReadyEvent> {

    private final DictionaryTemplate dictionaryTemplate;
    private final AmqpTemplate amqpTemplate;


    @Value("${spring.application.name:}")
    private String serviceName;

    @Autowired
    private JbmClusterEventRegistry jbmClusterEventRegistry;
    @Autowired
    private JbmClusterScheduledRegistry jbmClusterScheduledRegistry;

    public ClusterDicScan(DictionaryTemplate dictionaryTemplate, AmqpTemplate amqpTemplate) {
        this.dictionaryTemplate = dictionaryTemplate;
        this.amqpTemplate = amqpTemplate;
    }

    private AtomicBoolean lock = new AtomicBoolean(false);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (lock.getAndSet(true))
            return;
        if (ObjectUtil.isEmpty(this.amqpTemplate)) {
            log.warn("JBM节点无法连接MQ");
            return;
        }
        try {
            Map<String, List<JbmDictionary>> dics = dictionaryTemplate.getJbmDicMapCache();
            for (String key : dics.keySet()) {
                List<JbmDictionary> list = dics.get(key);
                if (CollUtil.isNotEmpty(list)) {
                    ThreadUtil.execAsync(new Runnable() {
                        @Override
                        public void run() {
                            amqpTemplate.convertAndSend(QueueConstants.QUEUE_SCAN_DIC_RESOURCE, list);
                        }
                    });
                }
            }
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    List<JbmClusterEventBean> registryBeanList = jbmClusterEventRegistry.getRegistryBeanList();
                    for (JbmClusterEventBean jbmClusterEventBean : registryBeanList) {
                        jbmClusterEventBean.setServiceName(serviceName);
                    }
                    amqpTemplate.convertAndSend(QueueConstants.QUEUE_SCAN_EVENT, registryBeanList);
                }
            });
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    List<JbmClusterEventBean> registryBeanList = jbmClusterScheduledRegistry.getRegistryBeanList();
                    for (JbmClusterEventBean jbmClusterEventBean : registryBeanList) {
                        jbmClusterEventBean.setServiceName(serviceName);
                    }
                    amqpTemplate.convertAndSend(QueueConstants.QUEUE_SCAN_SCHEDULED, registryBeanList);
                }
            });
        } catch (Exception e) {
            log.error("集群节点发送字典数据错误");
        }
    }


}
