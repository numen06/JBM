package com.jbm.cluster.common.basic.module;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.ClassUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.cluster.api.event.annotation.BusinessEvent;
import com.jbm.cluster.api.model.event.JbmClusterBusinessEventBean;
import com.jbm.cluster.common.basic.configuration.resources.JbmClusterBusinessEventScan;
import com.jbm.cluster.core.constant.QueueConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

/**
 * 自定义RestTemplate请求工具类
 *
 * @author: wesley.zhang
 * @date: 2018/12/11 15:51
 * @description:
 */
@Slf4j
public class JbmClusterBusinessEventTemplate {

    @Autowired
    private StreamBridge streamBridge;

    @Autowired
    private JbmClusterBusinessEventScan jbmClusterBusinessEventScan;

    /**
     * 发送业务事件
     *
     * @param obj
     * @param <T>
     */
    public <T> void sendBusinessEvent(T obj) {
        if (!AnnotationUtil.hasAnnotation(obj.getClass(), BusinessEvent.class)) {
            log.error("发送事件没有添加[BusinessEvent]注解");
            return;
        }
        final JbmClusterBusinessEventBean jbmClusterBusinessEventBean = new JbmClusterBusinessEventBean();
        final String code = ClassUtil.getClassName(obj.getClass(), false);
        jbmClusterBusinessEventBean.setEventCode(code);
        jbmClusterBusinessEventBean.setEventBody(JSON.toJSONString(obj));
        final Message<JbmClusterBusinessEventBean> message = MessageBuilder.withPayload(jbmClusterBusinessEventBean).build();
        streamBridge.send(QueueConstants.BUSINESS_EVENT_STREAM, message);
    }


}
