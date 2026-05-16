package com.jbm.cluster.auth.service;


import cn.dev33.satoken.oauth2.model.SaClientModel;
import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.cluster.common.satoken.oauth.ClientModelSource;
import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author wesley
 * @Created wesley.zhang
 * @Date 2022/5/15 13:08
 * @Description TODO
 */
@Slf4j
@Service
public class JbmPlatformClientModelSource implements ClientModelSource {

    @Autowired
    private BaseAppPreprocessing baseAppPreprocessing;

    @Override
    public int getOrder() {
        return -100;
    }

    // 根据 id 获取 Client 信息
    @Override
    public SaClientModel getClientModel(String clientId) {
        BaseApp baseApp;
        try {
            baseApp = baseAppPreprocessing.getAppByKey(clientId);
        } catch (Exception e) {
            log.debug("未找到已注册应用 clientId={}", clientId);
            return null;
        }
        if (ObjectUtil.isEmpty(baseApp)) {
            return null;
        }
        return new SaClientModel()
                .setClientId(baseApp.getApiKey())
                .setClientSecret(baseApp.getSecretKey())
                .setAllowUrl("*")
                .setContractScope("*")
                .setIsAutoMode(true);
    }


}