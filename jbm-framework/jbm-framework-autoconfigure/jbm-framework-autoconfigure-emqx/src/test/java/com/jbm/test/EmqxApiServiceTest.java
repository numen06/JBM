package com.jbm.test;

import cn.hutool.core.lang.Console;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import jbm.framework.boot.autoconfigure.emqx.EmqxApiService;
import jbm.framework.boot.autoconfigure.emqx.configuration.EmqxProperties;
import jbm.framework.boot.autoconfigure.emqx.model.EmqxClient;
import jbm.framework.boot.autoconfigure.emqx.model.EmqxSubscription;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

@Slf4j
public class EmqxApiServiceTest {


    private EmqxApiService emqxApiService;

    @BeforeEach
    public void before() {
        EmqxProperties emqxProperties = new EmqxProperties();
        emqxProperties.setUrl("http://10.100.10.121:18083");
        emqxProperties.setUsername("ee26b0dd4af7e749");
        emqxProperties.setPassword("jikw9C0MaA25NhbXFGvslDcyzwU0uZYeY9AnJlhY9Be8oE");
        emqxApiService = new EmqxApiService(emqxProperties);
    }

    @Test
    public void testGetOnlineAllClients() {
        List<EmqxClient> emqxClients = emqxApiService.getOnlineAllClients();
        Console.log("获取到{}个客户端", emqxClients.size());
        for (EmqxClient emqxClient : emqxClients) {
            log.info("{}", emqxClient);
        }
    }

    @Test
    public void testSelectClients() {
        EmqxClient sch = new EmqxClient();
        sch.setClientId("hivemq");
        List<EmqxClient> emqxClients = emqxApiService.selectClients(sch, new Page(1, 10));
        for (EmqxClient emqxClient : emqxClients) {
            Console.log(emqxClient);
        }
    }

    @Test
    public void testGetSubscriptionsByClient() {
        List<EmqxClient> emqxClients = emqxApiService.getOnlineAllClients();
        for (EmqxClient emqxClient : emqxClients) {
            if (emqxClient.getSubscriptionsCnt() <= 0) {
                continue;
            }
            List<EmqxSubscription> subscriptions = emqxApiService.getSubscriptionsByClient(emqxClient.getClientId());
            if (subscriptions.isEmpty()) {
                continue;
            }
            log.info("订阅关系:{},实际数量:{}", emqxClient.getSubscriptionsCnt(), subscriptions.size());
            Console.log(subscriptions);
        }
    }

    @Test
    public void testGetSubscriptionsByTopic() {
        Page page = new Page(1, 10);
        PageResult<EmqxSubscription> subscriptions = emqxApiService.getSubscriptionsByTopic("/iot/device/1934273851129925634/subDevices", page);
        for (EmqxSubscription subscription : subscriptions) {
            log.info("{}", subscription);
        }
    }

}
