package com.jbm.test;

import cn.hutool.core.lang.Console;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import jbm.framework.boot.autoconfigure.emqx.EmqxApiService;
import jbm.framework.boot.autoconfigure.emqx.configuration.EmqxConfiguration;
import jbm.framework.boot.autoconfigure.emqx.model.AuthUser;
import jbm.framework.boot.autoconfigure.emqx.model.AuthenticatorResult;
import jbm.framework.boot.autoconfigure.emqx.model.EmqxClient;
import jbm.framework.boot.autoconfigure.emqx.model.EmqxSubscription;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootConfiguration
@SpringBootTest(classes = {EmqxConfiguration.class})
public class EmqxApiServiceTest {

    @Autowired
    private EmqxApiService emqxApiService;

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
    public void testGetAuthentication() {
        List<AuthenticatorResult> authenticationList = emqxApiService.getAuthentication();
        for (AuthenticatorResult emqxApiService : authenticationList) {
            Console.log(emqxApiService);
        }
    }

    @Test
    public void testRegisterUser() {
        boolean flag = emqxApiService.registerUser(authId, clientId,null);
        if (flag) {
            Console.log("成功注册客户端:{}", clientId);
        } else {
            Console.log("注册客户端:{} 失败", clientId);
        }
    }
    String clientId = "test";
    String authId = "password_based:built_in_database";
    @Test
    public void testUnregisterUser() {

        boolean  flag = emqxApiService.unregisterUser(authId, clientId);
        if (flag) {
            Console.log("成功注销客户端:{}", clientId);
        } else {
            Console.log("注销客户端:{} 失败", clientId);
        }
    }

    @Test
    public void testRegAndUnRegClient() {
        this.testRegisterUser();
        this.testUnregisterUser();
    }

    @Test
    public void testGetAllAuthUser() {
        List<AuthUser> authUsers = emqxApiService.getAllAuthUser(authId);
        Console.log("获取所有认证用户:{}", authUsers);
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
