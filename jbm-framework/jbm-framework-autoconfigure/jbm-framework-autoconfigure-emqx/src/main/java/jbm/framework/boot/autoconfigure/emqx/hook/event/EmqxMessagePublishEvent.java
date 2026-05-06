package jbm.framework.boot.autoconfigure.emqx.hook.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * EMQX 消息发布事件：仅通知，无响应体反馈。
 */
@Getter
public class EmqxMessagePublishEvent extends ApplicationEvent {
    private final String clientid;
    private final String topic;
    private final String payload;

    public EmqxMessagePublishEvent(Object source, String clientid, String topic, String payload) {
        super(source);
        this.clientid = clientid;
        this.topic = topic;
        this.payload = payload;
    }
}
