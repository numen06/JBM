package com.jbm.framework.modbus.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author fanscat
 * @createTime 2024/5/29 22:01
 */
@Getter
public class ValueChangeEvent extends ApplicationEvent {

    private String clientId;
    private Integer offset;
    private Object value;

    public ValueChangeEvent(Object source, String clientId, Integer offset, Object value) {
        super(source);
        this.clientId = clientId;
        this.offset = offset;
        this.value = value;
    }
}
