package jbm.framework.boot.autoconfigure.emqx.hook.event;

import jbm.framework.boot.autoconfigure.emqx.hook.dto.EmqxAclRequest;
import jbm.framework.boot.autoconfigure.emqx.hook.dto.EmqxAclResponse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * EMQX ACL 请求事件：可携带 request；若采用事件反馈，监听器 setResponse 后 Controller 取回返回。
 */
@Getter
public class EmqxAclRequestEvent extends ApplicationEvent {
    private final EmqxAclRequest request;
    @Setter
    private EmqxAclResponse response;

    public EmqxAclRequestEvent(Object source, EmqxAclRequest request) {
        super(source);
        this.request = request;
    }
}
