package jbm.framework.boot.autoconfigure.emqx.hook;

import jbm.framework.boot.autoconfigure.emqx.hook.dto.EmqxAuthRequest;
import jbm.framework.boot.autoconfigure.emqx.hook.dto.EmqxAuthResponse;

/**
 * EMQX 认证钩子扩展点：业务实现此接口并返回 allow/deny，实现“业务处理再反馈”。
 */
public interface EmqxAuthHandler {
    EmqxAuthResponse auth(EmqxAuthRequest request);
}
