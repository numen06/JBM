package com.jbm.cluster.center.business.impl;

import cn.hutool.core.bean.BeanUtil;
import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.cluster.api.form.BaseAppForm;
import com.jbm.cluster.center.business.BaseAppBusiness;
import com.jbm.cluster.common.basic.JbmClusterTemplate;
import com.jbm.cluster.common.mysql.service.impl.BaseAppServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class BaseAppBusinessImpl extends BaseAppServiceImpl implements BaseAppBusiness {

    @Autowired
    private JbmClusterTemplate jbmClusterTemplate;

    @Override
    public BaseApp addAppWithGatewayRefresh(BaseAppForm form) {
        BaseApp app = BeanUtil.toBean(form, BaseApp.class);
        BaseApp result = addAppInfo(app);
        jbmClusterTemplate.refreshGateway();
        return result;
    }

    @Override
    public BaseApp updateAppWithGatewayRefresh(Long appId, BaseAppForm form) {
        BaseApp app = BeanUtil.toBean(form, BaseApp.class);
        app.setAppId(appId);
        BaseApp result = updateInfo(app);
        jbmClusterTemplate.refreshGateway();
        return result;
    }

    @Override
    public void removeAppWithGatewayRefresh(Long appId) {
        removeApp(appId);
        jbmClusterTemplate.refreshGateway();
    }

    @Override
    public String resetSecretWithGatewayRefresh(Long appId) {
        String secret = restSecret(appId);
        jbmClusterTemplate.refreshGateway();
        return secret;
    }
}