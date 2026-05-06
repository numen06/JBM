package jbm.framework.boot.autoconfigure.emqx.hook;

import jbm.framework.boot.autoconfigure.emqx.hook.dto.EmqxClientEventRequest;

/**
 * EMQX 客户端连接/断开钩子扩展点（可选）：若存在则 Controller 先调再发布事件。
 */
public interface EmqxClientEventHandler {
    void onConnected(EmqxClientEventRequest request);

    void onDisconnected(EmqxClientEventRequest request);
}
