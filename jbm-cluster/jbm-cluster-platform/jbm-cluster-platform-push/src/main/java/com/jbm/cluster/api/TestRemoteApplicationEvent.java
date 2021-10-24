package com.jbm.cluster.api;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;

@Data
@EqualsAndHashCode(callSuper = true)
public class TestRemoteApplicationEvent extends RemoteApplicationEvent {

    private String btime = "btime";


//    public TestRemoteApplicationEvent(Object source, String id, String o) {
//        super(source, id, o);
//    }
}
