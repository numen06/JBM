package com.jbm.cluster.common.satoken.core;

import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import com.jbm.framework.exceptions.auth.NotLoginException;

/**
 * @author fanscat
 * @createTime 2024/6/3 16:18
 */
public class StpLogicJwtForCustom extends StpLogicJwtForSimple {

    @Override
    public Object getLoginId() {
        // 如果正在[临时身份切换], 则返回临时身份
        if (isSwitch()) {
            return getSwitchLoginId();
        }
        NotLoginException.newInstance(loginType, NotLoginException.NOT_TOKEN);
        // 如果获取不到token，则抛出: 无token
        String tokenValue = getTokenValue();
        if (tokenValue == null) {
            throw NotLoginException.newInstance(loginType, NotLoginException.NOT_TOKEN);
        }
        // 查找此token对应loginId, 如果找不到则抛出：无效token
        String loginId = getLoginIdNotHandle(tokenValue);
        if (loginId == null) {
            throw NotLoginException.newInstance(loginType, NotLoginException.INVALID_TOKEN);
        }
        // 如果是已经过期，则抛出：已经过期
        if (loginId.equals(NotLoginException.TOKEN_TIMEOUT)) {
            throw NotLoginException.newInstance(loginType, NotLoginException.TOKEN_TIMEOUT);
        }
        // 如果是已经被顶替下去了, 则抛出：已被顶下线
        if (loginId.equals(NotLoginException.BE_REPLACED)) {
            throw NotLoginException.newInstance(loginType, NotLoginException.BE_REPLACED);
        }
        // 如果是已经被踢下线了, 则抛出：已被踢下线
        if (loginId.equals(NotLoginException.KICK_OUT)) {
            throw NotLoginException.newInstance(loginType, NotLoginException.KICK_OUT);
        }
        // 检查是否已经 [临时过期]
        checkActivityTimeout(tokenValue);
        // 如果配置了自动续签, 则: 更新[最后操作时间]
        if (getConfig().getAutoRenew()) {
            updateLastActivityToNow(tokenValue);
        }
        // 至此，返回loginId
        return loginId;
    }
}
