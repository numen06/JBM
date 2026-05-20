package com.jbm.cluster.center.business;

import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.cluster.api.form.BaseAppForm;
import com.jbm.cluster.common.mysql.service.BaseAppService;

public interface BaseAppBusiness extends BaseAppService {

    BaseApp addAppWithGatewayRefresh(BaseAppForm form);

    BaseApp updateAppWithGatewayRefresh(Long appId, BaseAppForm form);

    void removeAppWithGatewayRefresh(Long appId);

    String resetSecretWithGatewayRefresh(Long appId);
}