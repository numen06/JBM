package com.jbm.cluster.common.basic.service;

import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.core.constant.JbmConstants;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 登录失败提示文案：调试模式返回详细原因，非调试模式返回模糊提示。
 */
public class LoginErrorMessageService {

    @Autowired
    private SysDebugModeService sysDebugModeService;

    public String resolve(String detailMessage) {
        return resolve(detailMessage, JbmConstants.LOGIN_FAIL_VAGUE_MSG);
    }

    public String resolve(String detailMessage, String vagueMessage) {
        if (sysDebugModeService.isDebugModeEnabled()) {
            return StrUtil.emptyToDefault(detailMessage, vagueMessage);
        }
        return vagueMessage;
    }
}
