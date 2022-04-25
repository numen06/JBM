package com.jbm.framework.eventbus.example.event;


import jbm.framework.boot.autoconfigure.eventbus.model.AbstractClusterEvent;
import lombok.Data;

/**
 * @author wesley.zhang
 * @create 2021/7/19 10:53 上午
 * @email numen06@qq.com
 * @description
 */
@Data
public class DeviceUpdateEvent extends AbstractClusterEvent {

    private String device;

    private long ts;

    private String updateBy;

    private DeviceOnlineStatus online;

}
