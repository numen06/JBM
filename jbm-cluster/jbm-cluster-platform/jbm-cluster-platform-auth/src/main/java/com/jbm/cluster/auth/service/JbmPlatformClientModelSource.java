package com.jbm.cluster.auth.service;


import cn.dev33.satoken.oauth2.model.SaClientModel;
import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.cluster.common.satoken.oauth.ClientModelSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author wesley
 * @Created wesley.zhang
 * @Date 2022/5/15 13:08
 * @Description TODO
 */
@Slf4j
//@Service
public class JbmPlatformClientModelSource implements ClientModelSource {

    @Autowired
    private BaseAppPreprocessing baseAppPreprocessing;

    // 根据 id 获取 Client 信息
    @Override
    public SaClientModel getClientModel(String clientId) {
        BaseApp baseApp = baseAppPreprocessing.getAppByKey(clientId);
        return new SaClientModel()
                .setClientId(baseApp.getApiKey())
                .setClientSecret(baseApp.getSecretKey())
                .setAllowUrl("*")
                .setContractScope("*")
                .setIsAutoMode(true);
    }


}