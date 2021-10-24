package com.jbm.cluster.push.handler;

import com.alibaba.fastjson.JSON;
import com.jbm.cluster.api.TestClusterEvent;
import com.jbm.cluster.api.TestGClusterEvent;
import com.jbm.cluster.api.TestRemoteApplicationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TestEventHandler {
    @EventListener
    public void onUserRemoteApplicationEvent(TestClusterEvent event) {
        log.info(JSON.toJSONString(event));
    }

    @EventListener
    public void onUserRemoteApplicationEvent(TestGClusterEvent event) {
        log.info(JSON.toJSONString(event));
    }

    @EventListener
    public void onUserRemoteApplicationEvent2(TestRemoteApplicationEvent event) {
        log.info(JSON.toJSONString(event));
    }


//    @EventListener
//    public void onUserRemoteApplicationEvent2(RemoteApplicationEvent event) {
//        log.info(JSON.toJSONString(event));
//    }

//    @StreamListener(SpringCloudBusClient.INPUT)
//    public void acceptRemote(RemoteApplicationEvent event) {
//        log.info(JSON.toJSONString(event));
//    }
}