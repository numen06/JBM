package jbm.framework.boot.autoconfigure.emqx.hook;

import jbm.framework.boot.autoconfigure.emqx.hook.dto.EmqxAclRequest;
import jbm.framework.boot.autoconfigure.emqx.hook.dto.EmqxAclResponse;

/**
 * EMQX ACL 钩子扩展点：业务实现此接口并返回 allow/deny，实现“业务处理再反馈”。
 */
public interface EmqxAclHandler {
    EmqxAclResponse acl(EmqxAclRequest request);
}
