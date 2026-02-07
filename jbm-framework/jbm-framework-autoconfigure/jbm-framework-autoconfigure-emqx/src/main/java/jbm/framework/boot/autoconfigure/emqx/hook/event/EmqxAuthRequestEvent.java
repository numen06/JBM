package jbm.framework.boot.autoconfigure.emqx.hook.event;

import jbm.framework.boot.autoconfigure.emqx.hook.dto.EmqxAuthRequest;
import jbm.framework.boot.autoconfigure.emqx.hook.dto.EmqxAuthResponse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * EMQX 认证请求事件：可携带 request；若采用事件反馈，监听器 setResponse 后 Controller 取回返回。
 */
@Getter
public class EmqxAuthRequestEvent extends ApplicationEvent {
    private final EmqxAuthRequest request;
    @Setter
    private EmqxAuthResponse response;

    public EmqxAuthRequestEvent(Object source, EmqxAuthRequest request) {
        super(source);
        this.request = request;
    }
}
