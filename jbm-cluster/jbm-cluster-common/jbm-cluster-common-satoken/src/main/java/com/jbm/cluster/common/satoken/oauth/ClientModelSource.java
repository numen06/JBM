package com.jbm.cluster.common.satoken.oauth;

import cn.dev33.satoken.oauth2.model.SaClientModel;

/**
 * 节点获取客户端token的代码
 */
public interface ClientModelSource {


    SaClientModel getClientModel(String clientId);
}
