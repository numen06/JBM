package com.jbm.cluster.center.business.impl;

import cn.hutool.core.bean.BeanUtil;
import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.cluster.api.form.BaseAppForm;
import com.jbm.cluster.center.business.BaseAppBusiness;
import com.jbm.cluster.common.basic.JbmClusterTemplate;
import com.jbm.cluster.common.mysql.service.BaseAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BaseAppBusinessImpl implements BaseAppBusiness {

    @Autowired
    private BaseAppService baseAppService;
    @Autowired
    private JbmClusterTemplate jbmClusterTemplate;

    @Override
    public BaseApp addAppWithGatewayRefresh(BaseAppForm form) {
        BaseApp app = BeanUtil.toBean(form, BaseApp.class);
        BaseApp result = baseAppService.addAppInfo(app);
        jbmClusterTemplate.refreshGateway();
        return result;
    }

    @Override
    public BaseApp updateAppWithGatewayRefresh(Long appId, BaseAppForm form) {
        BaseApp app = BeanUtil.toBean(form, BaseApp.class);
        app.setAppId(appId);
        BaseApp result = baseAppService.updateInfo(app);
        jbmClusterTemplate.refreshGateway();
        return result;
    }

    @Override
    public void removeAppWithGatewayRefresh(Long appId) {
        baseAppService.removeApp(appId);
        jbmClusterTemplate.refreshGateway();
    }

    @Override
    public String resetSecretWithGatewayRefresh(Long appId) {
        String secret = baseAppService.restSecret(appId);
        jbmClusterTemplate.refreshGateway();
        return secret;
    }
}
