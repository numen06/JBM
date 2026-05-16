package com.jbm.cluster.common.satoken.oauth;

import cn.dev33.satoken.oauth2.model.SaClientModel;

/**
 * 节点获取客户端token的代码
 */
public interface ClientModelSource {

    /**
     * 越小越优先（开发者应用应优先于节点自动 ClientModel）
     */
    default int getOrder() {
        return 0;
    }

    SaClientModel getClientModel(String clientId);
}
