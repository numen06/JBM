package com.jbm.cluster.common.mysql.service;

import com.jbm.cluster.api.entitys.basic.BaseApiKey;
import com.jbm.cluster.api.form.BaseApiKeyForm;
import com.jbm.cluster.api.model.auth.OpenAuthority;
import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.usage.paging.DataPaging;

import java.util.Date;
import java.util.List;

/**
 * 第三方 API Key 管理
 */
public interface BaseApiKeyService extends IMasterDataService<BaseApiKey> {

    DataPaging<BaseApiKey> findListPage(BaseApiKeyForm form);

    BaseApiKey getByKeyId(Long keyId);

    BaseApiKey getByApiKey(String apiKey);

    /**
     * 创建 API Key，返回实体（含明文 secretKey 仅此次返回）
     */
    BaseApiKey createApiKey(BaseApiKeyForm form, Long operatorUserId);

    BaseApiKey updateApiKey(Long keyId, BaseApiKeyForm form);

    String resetSecret(Long keyId);

    void updateStatus(Long keyId, Integer status);

    void removeApiKey(Long keyId);

    List<OpenAuthority> findAuthorityByKeyId(Long keyId);

    void grantAuthority(Long keyId, Long operatorUserId, Date expireTime, String... authorityIds);

    /**
     * 校验 API Key 是否有权访问指定 authorityId
     */
    boolean hasAuthority(Long keyId, Long authorityId);

    /**
     * 按 apiId 校验（请求路径对应 base_api）
     */
    boolean hasAuthorityForApi(Long keyId, Long apiId);

    void touchLastUsed(Long keyId);

    /**
     * 当前用户可授权给 API Key 的 API 权限列表
     */
    List<OpenAuthority> findGrantableAuthorities(Long operatorUserId);
}
