package com.jbm.framework.opcua.listener;

import com.jbm.framework.opcua.OpcUaClientBean;
import com.jbm.framework.opcua.OpcUaTemplate;
import com.jbm.framework.opcua.attribute.OpcPoint;
import com.jbm.framework.opcua.attribute.SubscriptionPoint;
import com.jbm.framework.opcua.event.ValueChanageEvent;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscriptionManager;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;

/**
 *
 */
@Slf4j
public class GuardSubscriptionListener implements UaSubscriptionManager.SubscriptionListener {

    private final OpcUaTemplate opcUaTemplate;
    private final OpcUaClientBean opcUaClientBean;


    public GuardSubscriptionListener(OpcUaTemplate opcUaTemplate, OpcUaClientBean opcUaClientBean) {
        this.opcUaTemplate = opcUaTemplate;
        this.opcUaClientBean = opcUaClientBean;
    }

    public void onKeepAlive(UaSubscription subscription, DateTime publishTime) {
//        log.info("onKeepAlive");
    }

    public void onStatusChanged(UaSubscription subscription, StatusCode status) {
//        log.info("onStatusChanged");
    }

    public void onPublishFailure(UaException exception) {
//        log.info("onPublishFailure");
    }

    public void onNotificationDataLost(UaSubscription subscription) {
//        log.info("onNotificationDataLost");
    }

    //重连时 尝试恢复之前的订阅失败时 会调用此方法
    public void onSubscriptionTransferFailed(UaSubscription subscription, StatusCode statusCode) {
        log.info("设备[{}]订阅失效，开始重新订阅，数量为:{}", opcUaClientBean.getDeviceId(), opcUaClientBean.getSubscriptionPoints().size());
        for (String pointCode : opcUaClientBean.getSubscriptionPoints().keySet()) {
            OpcPoint opcPoint = opcUaClientBean.getPoints().get(pointCode);
            ValueChanageEvent valueChanageEvent = opcUaClientBean.getSubscriptionPoints().get(pointCode);
            opcUaTemplate.subscribeItem(opcUaClientBean.getDeviceId(), opcPoint, valueChanageEvent);
        }
        log.info("设备[{}]重新订阅完成", opcUaClientBean.getDeviceId());
    }
}
