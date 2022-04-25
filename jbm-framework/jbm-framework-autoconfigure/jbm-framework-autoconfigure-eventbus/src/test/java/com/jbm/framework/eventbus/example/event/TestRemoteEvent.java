package com.jbm.framework.eventbus.example.event;

import jbm.framework.boot.autoconfigure.eventbus.model.AbstractClusterEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class TestRemoteEvent extends AbstractClusterEvent {

    private String title;

    private String msg;

    public TestRemoteEvent() {
    }


}
