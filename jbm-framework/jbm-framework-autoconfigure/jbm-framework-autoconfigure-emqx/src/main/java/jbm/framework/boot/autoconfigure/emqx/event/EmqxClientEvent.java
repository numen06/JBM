package jbm.framework.boot.autoconfigure.emqx.event;


import jbm.framework.boot.autoconfigure.emqx.EmqxClientStatus;
import jbm.framework.boot.autoconfigure.emqx.model.EmqxClient;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author wesley
 */
public class EmqxClientEvent extends ApplicationEvent {

    @Getter
    private final EmqxClient emqxClient;
    // "online" or "offline"
    @Getter
    private final EmqxClientStatus status;

    public EmqxClientEvent(EmqxClient source, EmqxClientStatus status) {
        super(source);
        this.emqxClient = source;
        this.status = status;
    }


}
