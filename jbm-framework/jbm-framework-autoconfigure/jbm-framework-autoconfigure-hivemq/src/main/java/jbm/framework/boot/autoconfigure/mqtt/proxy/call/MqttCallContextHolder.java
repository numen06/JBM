package jbm.framework.boot.autoconfigure.mqtt.proxy.call;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jbm.framework.boot.autoconfigure.mqtt.useage.MqttCallContext;

import java.util.concurrent.TimeUnit;

/**
 * @author wesley
 */
public class MqttCallContextHolder {

    private static final ThreadLocal<MqttCallContext> CONTEXT_HOLDER = new ThreadLocal<>();

    private static final Cache<String, MqttCallContext> CONTEXT_CACHE = Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();

    public static void set(MqttCallContext context) {
        CONTEXT_HOLDER.set(context);
//        if (CONTEXT_CACHE.getIfPresent(context.getEventId()) == null) {
            CONTEXT_CACHE.put(context.getEventId(), context);
//        }
    }

    public static MqttCallContext get() {
        return CONTEXT_HOLDER.get();
    }

    public static MqttCallContext get(String eventId) {
        return CONTEXT_CACHE.getIfPresent(eventId);
    }


}
