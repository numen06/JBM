package com.jbm.cluster.api.event;


import com.jbm.cluster.api.event.annotation.BusinessEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@BusinessEvent(name = "测试业务事件")
public class TestBusinessEvent {

    /**
     * 我是内容
     */
    private String content;
}
