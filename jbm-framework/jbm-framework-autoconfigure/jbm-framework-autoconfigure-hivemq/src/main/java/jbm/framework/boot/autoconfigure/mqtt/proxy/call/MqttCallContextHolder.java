package jbm.framework.boot.autoconfigure.mqtt.proxy.call;

import jbm.framework.boot.autoconfigure.mqtt.useage.MqttCallContext;

/**
 * @author wesley
 */
public class MqttCallContextHolder {

    private static final ThreadLocal<MqttCallContext> CONTEXT_HOLDER = new ThreadLocal<>();

    public static void set(MqttCallContext context) {
        CONTEXT_HOLDER.set(context);
    }

    public static MqttCallContext get() {
        return CONTEXT_HOLDER.get();
    }
}
