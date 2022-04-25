package com.jbm.cluster.center.event;

import com.jbm.cluster.api.model.entity.BaseMenu;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public class NewMenuEvent extends ApplicationEvent {

    @Getter
    private final BaseMenu baseMenu;

    public NewMenuEvent(Object source, BaseMenu baseMenu) {
        super(source);
        this.baseMenu = baseMenu;
    }

}
