package com.jbm.cluster.node.configuration.cluster;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.jbm.autoconfig.dic.DictionaryTemplate;
import com.jbm.cluster.common.constants.QueueConstants;
import com.jbm.framework.dictionary.JbmDictionary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.util.List;
import java.util.Map;

@Slf4j
public class ClusterDicScan implements ApplicationListener<ApplicationReadyEvent> {

    private final DictionaryTemplate dictionaryTemplate;
    private final AmqpTemplate amqpTemplate;

    public ClusterDicScan(DictionaryTemplate dictionaryTemplate, AmqpTemplate amqpTemplate) {
        this.dictionaryTemplate = dictionaryTemplate;
        this.amqpTemplate = amqpTemplate;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
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
        } catch (Exception e) {
            log.error("集群节点发送字典数据错误");
        }
    }
}
