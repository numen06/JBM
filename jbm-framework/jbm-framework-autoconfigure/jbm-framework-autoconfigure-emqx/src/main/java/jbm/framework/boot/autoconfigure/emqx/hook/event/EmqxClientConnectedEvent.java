package jbm.framework.boot.autoconfigure.emqx.hook.event;

import jbm.framework.boot.autoconfigure.emqx.hook.dto.EmqxClientEventRequest;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * EMQX 客户端连接事件：仅通知，无响应体反馈。
 */
@Getter
public class EmqxClientConnectedEvent extends ApplicationEvent {
    private final EmqxClientEventRequest request;

    public EmqxClientConnectedEvent(Object source, EmqxClientEventRequest request) {
        super(source);
        this.request = request;
    }
}
