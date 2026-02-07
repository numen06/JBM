package jbm.framework.boot.autoconfigure.emqx.hook;

/**
 * EMQX 消息发布钩子扩展点（可选）：若存在则 Controller 先调再发布事件。
 */
public interface EmqxMessagePublishHandler {
    void onPublish(String clientid, String topic, String payload);
}
