package com.jbm.cluster.core.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.basic.BaseAccount;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.metadata.bean.ResultBody;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 密码过期/预警 extra 字段计算
 */
public final class PasswordExpiryUtils {

    public static final String KEY_PASSWORD_NEED_CHANGE = "passwordNeedChange";
    public static final String KEY_PASSWORD_EXPIRED = "passwordExpired";
    public static final String KEY_PASSWORD_EXPIRE_DAYS = "passwordExpireDays";
    public static final String EXPIRED_MESSAGE = "密码已过期，请尽快修改密码";

    private PasswordExpiryUtils() {
    }

    public static Date resolvePasswordUpdateTime(BaseAccount account) {
        if (account == null) {
            return null;
        }
        return ObjectUtil.defaultIfNull(account.getPasswordUpdateTime(),
                ObjectUtil.defaultIfNull(account.getUpdateTime(), account.getCreateTime()));
    }

    public static Date resolveLatestPasswordUpdateTime(List<BaseAccount> accounts) {
        if (CollUtil.isEmpty(accounts)) {
            return null;
        }
        Date latestUpdate = null;
        for (BaseAccount account : accounts) {
            if (StrUtil.isBlank(account.getPassword())) {
                continue;
            }
            Date pwdTime = resolvePasswordUpdateTime(account);
            if (pwdTime != null && (latestUpdate == null || pwdTime.after(latestUpdate))) {
                latestUpdate = pwdTime;
            }
        }
        return latestUpdate;
    }

    public static Map<String, Object> buildExpiryExtra(List<BaseAccount> accounts) {
        Date latestUpdate = resolveLatestPasswordUpdateTime(accounts);
        if (latestUpdate == null) {
            return Collections.emptyMap();
        }
        long days = DateUtil.betweenDay(latestUpdate, DateTime.now(), false);
        Map<String, Object> extra = new HashMap<>();
        if (days >= JbmConstants.PASSWORD_EXPIRE_DAYS) {
            extra.put(KEY_PASSWORD_NEED_CHANGE, true);
            extra.put(KEY_PASSWORD_EXPIRED, true);
            extra.put(KEY_PASSWORD_EXPIRE_DAYS, 0);
        } else if (days >= JbmConstants.PASSWORD_WARN_DAYS) {
            extra.put(KEY_PASSWORD_NEED_CHANGE, false);
            extra.put(KEY_PASSWORD_EXPIRED, false);
            extra.put(KEY_PASSWORD_EXPIRE_DAYS, JbmConstants.PASSWORD_EXPIRE_DAYS - days);
        }
        return extra;
    }

    public static void applyExpiryExtra(ResultBody<?> resultBody, List<BaseAccount> accounts) {
        if (resultBody == null) {
            return;
        }
        Map<String, Object> extra = buildExpiryExtra(accounts);
        extra.forEach(resultBody::put);
        if (Boolean.TRUE.equals(extra.get(KEY_PASSWORD_NEED_CHANGE))) {
            resultBody.msg(EXPIRED_MESSAGE);
        }
    }
}
