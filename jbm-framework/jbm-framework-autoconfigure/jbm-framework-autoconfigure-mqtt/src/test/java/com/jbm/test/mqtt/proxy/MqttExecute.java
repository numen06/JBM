package com.jbm.test.mqtt.proxy;

import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttRequest;

public interface MqttExecute {

    public void from();

    public String to(String msg);
}
