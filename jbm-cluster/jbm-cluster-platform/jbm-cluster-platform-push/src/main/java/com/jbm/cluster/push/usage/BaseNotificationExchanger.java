package com.jbm.cluster.push.usage;

import cn.hutool.core.util.TypeUtil;
import com.jbm.cluster.api.constants.push.PushStatus;
import com.jbm.cluster.api.entitys.message.Notification;
import com.jbm.cluster.api.entitys.message.PushMessageBody;
import com.jbm.cluster.api.entitys.message.PushMessageItem;
import com.jbm.cluster.api.model.push.PushCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;

/**
 * @Created wesley.zhang
 * @Date 2022/4/25 13:19
 * @Description TODO
 */
public abstract class BaseNotificationExchanger<T extends Notification> implements NotificationExchanger<T> {

    private Class baseType;

    @Autowired
    private StreamBridge streamBridge;

    @Override
    public boolean support(Object notification) {
        if (notification == null) {
            return false;
        }
        if (baseType == null) {
            baseType = TypeUtil.getClass(TypeUtil.getTypeArgument(getClass(), 0));
        }
        return baseType.equals(notification.getClass());
    }

    /**
     * 发送回调
     *
     * @param notification
     */
    @Override
    public void send(T notification) {
        try {
            PushCallback pushCallback = this.apply(notification);
            this.sendCallBack(pushCallback);
        } catch (Exception e) {
            PushCallback pushCallback = this.error(notification, "", e.getMessage());
            this.sendCallBack(pushCallback);
        }
    }


    /**
     * 通过消息体和消息条目生产发送实体
     *
     * @param pushMessageBody
     * @param pushMessageItem
     * @return
     */
    @Override
    public T build(PushMessageBody pushMessageBody, PushMessageItem pushMessageItem) {
        return null;
    }

    /**
     * 发送回调
     *
     * @param pushCallback
     * @return
     */
    public boolean sendCallBack(PushCallback pushCallback) {
        return streamBridge.send("pushCallBack-in-0", pushCallback);
    }

    /**
     * 发送成功回调
     *
     * @param notification
     * @return
     */
    public PushCallback success(T notification) {
        PushCallback pushCallback = new PushCallback();
        pushCallback.setMsgId(notification.getMsgId());
        pushCallback.setPushStatus(PushStatus.issued);
        return pushCallback;
    }

    /**
     * 发送失败回调
     *
     * @param notification
     * @param code
     * @param msg
     * @return
     */

    public PushCallback error(T notification, String code, String msg) {
        PushCallback pushCallback = new PushCallback();
        pushCallback.setErrorCode(code);
        pushCallback.setErrorMsg(msg);
        pushCallback.setPushStatus(PushStatus.fail);
        return pushCallback;
    }


    //    @Override
//    public boolean exchange(T notification) {
//        try {
//            this.accept(notification);
//            return true;
//        } catch (Exception e) {
//            return false;
//        }
//    }


}
