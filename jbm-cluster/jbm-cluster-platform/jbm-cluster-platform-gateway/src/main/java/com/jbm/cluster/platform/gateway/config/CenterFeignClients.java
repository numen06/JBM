package com.jbm.cluster.platform.gateway.config;

import com.jbm.cluster.api.service.feign.IBaseApiFeignClient;
import com.jbm.cluster.api.service.feign.IBaseApiKeyFeignClient;
import com.jbm.cluster.api.service.feign.client.BaseApiKeyServiceClient;
import com.jbm.cluster.api.service.feign.client.BaseApiServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * jaja7 优先走本地直连 Center；其它环境回退 Feign/Nacos。
 */
@Component
public class CenterFeignClients {

  @Autowired(required = false)
  private Jaja7LocalCenterClient localCenterClient;

  @Autowired
  private BaseApiKeyServiceClient baseApiKeyServiceClient;

  @Autowired
  private BaseApiServiceClient baseApiServiceClient;

  public IBaseApiKeyFeignClient apiKey() {
    if (localCenterClient != null) {
      return new IBaseApiKeyFeignClient() {
        @Override
        public com.jbm.cluster.api.entitys.basic.BaseApiKey getByApiKey(String apiKey) {
          return localCenterClient.getByApiKey(apiKey);
        }

        @Override
        public com.jbm.cluster.api.entitys.basic.BaseApiKey getByKeyId(Long keyId) {
          return baseApiKeyServiceClient.getByKeyId(keyId);
        }

        @Override
        public Boolean checkAuthority(Long keyId, Long apiId) {
          return localCenterClient.checkAuthority(keyId, apiId);
        }
      };
    }
    return baseApiKeyServiceClient;
  }

  public IBaseApiFeignClient api() {
    if (localCenterClient != null) {
      return new IBaseApiFeignClient() {
        @Override
        public java.util.List<com.jbm.cluster.api.entitys.basic.BaseApi> getApiAllList(String serviceId) {
          return baseApiServiceClient.getApiAllList(serviceId);
        }

        @Override
        public com.jbm.cluster.api.entitys.basic.BaseApi findApiByPath(String serviceId, String path) {
          return localCenterClient.findApiByPath(serviceId, path);
        }

        @Override
        public com.jbm.cluster.api.entitys.basic.BaseApi getApi(Long apiId) {
          return baseApiServiceClient.getApi(apiId);
        }
      };
    }
    return baseApiServiceClient;
  }
}
