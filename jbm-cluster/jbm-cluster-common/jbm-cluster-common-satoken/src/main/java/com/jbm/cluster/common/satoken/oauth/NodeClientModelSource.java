package com.jbm.cluster.common.satoken.oauth;

import cn.dev33.satoken.oauth2.model.SaClientModel;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;

import java.util.UUID;


public class NodeClientModelSource implements ClientModelSource {


    /**
     * 根据 clientId 获取客户端模型
     *
     * @param clientId 客户端 ID
     * @return 客户端模型对象 SaClientModel，如果 clientId 包含了应用名称则返回一个新的 SaClientModel 对象，否则返回 null
     */
    @Override
    public SaClientModel getClientModel(String clientId) {
        // 如果 clientId 包含了应用名称
        if (StrUtil.contains(clientId, SpringUtil.getApplicationName())) {
            // 返回一个新的 SaClientModel 对象
            return new SaClientModel()
                    // 设置 clientId
                    .setClientId(clientId)
                    // 设置 clientSecret 为一个随机字符串
                    .setClientSecret(UUID.randomUUID().toString())
                    // 设置 allowUrl 为通配符 *
                    .setAllowUrl("*")
                    // 设置 contractScope 为通配符 *
                    .setContractScope("*")
                    // 设置 isAutoMode 为 true
                    .setIsAutoMode(true);
        }
        // 返回 null
        return null;
    }

}
