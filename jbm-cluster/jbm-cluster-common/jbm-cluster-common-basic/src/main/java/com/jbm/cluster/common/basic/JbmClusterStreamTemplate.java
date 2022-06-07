package com.jbm.cluster.common.basic;

import com.jbm.cluster.api.entitys.basic.BaseAccountLogs;
import com.jbm.cluster.api.model.api.JbmApiResource;
import com.jbm.cluster.api.model.gateway.GatewayLogInfo;
import com.jbm.cluster.core.constant.QueueConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * 自定义RestTemplate请求工具类
 *
 * @author: wesley.zhang
 * @date: 2018/12/11 15:51
 * @description:
 */
@Slf4j
public class JbmClusterStreamTemplate {

    @Autowired
    private StreamBridge streamBridge;

    /**
     * 发送访问日志
     *
     * @param gatewayLogs
     */
    public void sendAccessLogs(GatewayLogInfo gatewayLogs) {
        final Message<GatewayLogInfo> message = MessageBuilder.withPayload(gatewayLogs).build();
        //发送数据
        streamBridge.send(QueueConstants.ACCESS_LOGS_STREAM, message);
    }

    /**
     * 发送登录日志
     *
     * @param accountLogs
     */
    public void sendAccountLogs(BaseAccountLogs accountLogs) {
        final Message<BaseAccountLogs> message = MessageBuilder.withPayload(accountLogs).build();
        //发送数据
        streamBridge.send(QueueConstants.ACCOUNT_LOGS_STREAM, message);
    }

    /**
     * 发送登录日志
     *
     * @param jbmApiResource
     */
    public void sendApiResource(JbmApiResource jbmApiResource) {
        final Message<JbmApiResource> message = MessageBuilder.withPayload(jbmApiResource).build();
        //发送数据
        streamBridge.send(QueueConstants.API_RESOURCE_STREAM, message);
    }


}
