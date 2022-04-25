package com.jbm.framework.eventbus.example.consumer;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.IdUtil;
import com.jbm.framework.eventbus.example.event.TestRemoteEvent;
import jbm.framework.boot.autoconfigure.eventbus.publisher.ClusterEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * @author wesley.zhang
 * @create 2021/7/21 8:09 下午
 * @email numen06@qq.com
 * @description
 */
@Service
@Slf4j
public class TestBusService {

    @Autowired
    private ClusterEventPublisher clusterEventPublisher;


    @Scheduled(cron = "0/10 * *  * * ? ")
    public void sendTestClusterEvent() {
        if (!BooleanUtil.toBoolean(System.getProperty("send")))
            return;
        TestRemoteEvent remoteEvent = new TestRemoteEvent();
        remoteEvent.setTitle("MSG" + DateUtil.now());
        remoteEvent.setMsg(IdUtil.fastSimpleUUID());
        try {
            clusterEventPublisher.publishEvent(remoteEvent);
            log.info("发送成功:{}", remoteEvent.getTitle());
        } catch (Exception e) {
            log.error("发生错误", e);
        }
    }

}
