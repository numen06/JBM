package com.jbm.cluster.center.business;

import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.cluster.api.form.BaseAppForm;

public interface BaseAppBusiness {

    BaseApp addAppWithGatewayRefresh(BaseAppForm form);

    BaseApp updateAppWithGatewayRefresh(Long appId, BaseAppForm form);

    void removeAppWithGatewayRefresh(Long appId);

    String resetSecretWithGatewayRefresh(Long appId);

    String getPlainSecret(Long appId);
}
