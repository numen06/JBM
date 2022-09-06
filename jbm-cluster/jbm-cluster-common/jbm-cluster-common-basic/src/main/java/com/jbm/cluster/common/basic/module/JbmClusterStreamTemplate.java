package com.jbm.cluster.common.basic.module;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.basic.BaseAccountLogs;
import com.jbm.cluster.api.model.api.JbmApiResource;
import com.jbm.cluster.api.model.dic.JbmDicResource;
import com.jbm.cluster.api.model.gateway.GatewayLogInfo;
import com.jbm.cluster.core.constant.QueueConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import java.util.List;

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


    public <T> void sendToStream(T obj) {
        final Message<T> message = MessageBuilder.withPayload(obj).build();
        String clsName = StrUtil.lowerFirst(ClassUtil.getClassName(obj, true));
        //发送数据
        streamBridge.send(StrUtil.format("{}-in-0", clsName), message);
    }

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

    public void sendJbmDicResource(JbmDicResource jbmDicResource) {
        final Message<JbmDicResource> message = MessageBuilder.withPayload(jbmDicResource).build();
        //发送数据
        streamBridge.send(QueueConstants.DIC_RESOURCE_STREAM, message);
    }

    public <T> void sendResources(String stream, List<T> resources) {
        final Message<List<T>> message = MessageBuilder.withPayload(resources).build();
        //发送数据
        streamBridge.send(stream, message);
    }


    public <T> void sendResource(String stream, T resource) {
        final Message<T> message = MessageBuilder.withPayload(resource).build();
        //发送数据
        streamBridge.send(stream, message);
    }

}
