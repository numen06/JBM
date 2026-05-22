package com.jbm.cluster.auth.service;

import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.constants.RequestDeviceType;
import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.framework.exceptions.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LoginPostProcessor {

    @Autowired
    private BaseAppPreprocessing baseAppPreprocessing;

    public void enrichLoginUser(JbmLoginUser loginUser, String clientId) {
        if (StrUtil.isBlank(clientId)) {
            throw new ServiceException("client_id 不能为空");
        }
        BaseApp baseApp = baseAppPreprocessing.getAppByKey(clientId);
        loginUser.setAppId(baseApp.getAppId());
        loginUser.setClientId(clientId);
        if (StrUtil.isBlank(loginUser.getDevice())) {
            loginUser.setDevice(RequestDeviceType.PC.getDevice());
        }
    }
}
