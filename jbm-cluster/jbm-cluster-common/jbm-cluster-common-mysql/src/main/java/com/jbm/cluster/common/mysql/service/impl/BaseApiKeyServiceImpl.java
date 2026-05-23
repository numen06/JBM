package com.jbm.cluster.common.mysql.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.RSA;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.jbm.cluster.api.entitys.basic.BaseApi;
import com.jbm.cluster.api.entitys.basic.BaseApiKey;
import com.jbm.cluster.api.entitys.basic.BaseAuthority;
import com.jbm.cluster.api.entitys.basic.BaseAuthorityApikey;
import com.jbm.cluster.api.entitys.basic.BaseDeveloper;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.form.BaseApiKeyForm;
import com.jbm.cluster.api.model.auth.OpenAuthority;
import com.jbm.cluster.common.mysql.mapper.BaseApiKeyMapper;
import com.jbm.cluster.common.mysql.mapper.BaseAuthorityApikeyMapper;
import com.jbm.cluster.common.mysql.service.BaseApiKeyService;
import com.jbm.cluster.common.mysql.service.BaseApiService;
import com.jbm.cluster.common.mysql.service.BaseAuthorityService;
import com.jbm.cluster.common.mysql.service.BaseDeveloperService;
import com.jbm.cluster.common.mysql.service.BaseUserService;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import com.jbm.cluster.core.constant.ApiKeyConstants;
import com.jbm.cluster.core.constant.JbmCacheConstants;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.masterdata.usage.CriteriaQueryWrapper;
import com.jbm.framework.masterdata.usage.PageParams;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import com.jbm.util.RandomValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyPair;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BaseApiKeyServiceImpl extends MasterDataServiceImpl<BaseApiKey> implements BaseApiKeyService {

    @Autowired
    private BaseApiKeyMapper baseApiKeyMapper;
    @Autowired
    private BaseAuthorityApikeyMapper baseAuthorityApikeyMapper;
    @Autowired
    private BaseAuthorityService baseAuthorityService;
    @Autowired
    private BaseDeveloperService baseDeveloperService;
    @Autowired
    private BaseUserService baseUserService;
    @Autowired
    private BaseApiService baseApiService;
    @Autowired
    @Lazy
    private BaseApiKeyService self;

    @Override
    public DataPaging<BaseApiKey> findListPage(BaseApiKeyForm form) {
        PageForm pageForm = form.getPageForm() != null ? form.getPageForm() : new PageForm();
        CriteriaQueryWrapper<BaseApiKey> cq = CriteriaQueryWrapper.from(PageParams.from(pageForm));
        cq.lambda()
                .eq(ObjectUtils.isNotEmpty(form.getDeveloperId()), BaseApiKey::getDeveloperId, form.getDeveloperId())
                .eq(ObjectUtils.isNotEmpty(form.getBizAppId()), BaseApiKey::getBizAppId, form.getBizAppId())
                .eq(ObjectUtils.isNotEmpty(form.getStatus()), BaseApiKey::getStatus, form.getStatus())
                .likeRight(ObjectUtils.isNotEmpty(form.getKeyName()), BaseApiKey::getKeyName, form.getKeyName());
        cq.orderByDesc("create_time");
        return selectEntitys(cq);
    }

    @Override
    public BaseApiKey getByKeyId(Long keyId) {
        return baseApiKeyMapper.selectById(keyId);
    }

    @Cacheable(value = JbmCacheConstants.API_KEY_CACHE_NAMESPACE, key = "#apiKey", unless = "#result == null")
    @Override
    public BaseApiKey getByApiKey(String apiKey) {
        if (StrUtil.isBlank(apiKey)) {
            throw new ServiceException("apiKey 为空");
        }
        QueryWrapper<BaseApiKey> q = new QueryWrapper<>();
        q.lambda().eq(BaseApiKey::getApiKey, apiKey.trim());
        BaseApiKey row = CollUtil.getFirst(baseApiKeyMapper.selectList(q));
        if (row == null) {
            throw ServiceException.of("API Key 不存在: " + apiKey);
        }
        ensureRsaKeyPair(row);
        return row;
    }

    @CacheEvict(value = JbmCacheConstants.API_KEY_CACHE_NAMESPACE, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public BaseApiKey createApiKey(BaseApiKeyForm form, Long operatorUserId) {
        assertDeveloperActive(operatorUserId);
        String plainSecret = RandomValueUtils.randomAlphanumeric(32);
        String apiKey = RandomValueUtils.randomAlphanumeric(24);
        BaseApiKey entity = new BaseApiKey();
        entity.setDeveloperId(operatorUserId != null ? operatorUserId : form.getDeveloperId());
        entity.setBizAppId(form.getBizAppId());
        entity.setKeyName(form.getKeyName());
        entity.setKeyDesc(form.getKeyDesc());
        entity.setClientName(form.getClientName());
        entity.setScopeModules(form.getScopeModules());
        entity.setExpireTime(form.getExpireTime());
        entity.setApiKey(apiKey);
        entity.setSecretKey(SecurityUtils.encryptPassword(plainSecret));
        entity.setStatus(form.getStatus() != null ? form.getStatus() : ApiKeyConstants.API_KEY_STATUS_ENABLED);
        entity.setCreateTime(new Date());
        entity.setUpdateTime(entity.getCreateTime());
        baseApiKeyMapper.insert(entity);
        ensureRsaKeyPair(entity);
        entity.setSecretKey(plainSecret);
        if (CollUtil.isNotEmpty(form.getAuthorityIds())) {
            grantAuthority(entity.getKeyId(), operatorUserId, form.getAuthorityExpireTime(),
                    form.getAuthorityIds().toArray(new String[0]));
        }
        return entity;
    }

    @CacheEvict(value = JbmCacheConstants.API_KEY_CACHE_NAMESPACE, allEntries = true)
    @Override
    public BaseApiKey updateApiKey(Long keyId, BaseApiKeyForm form) {
        BaseApiKey existing = getByKeyId(keyId);
        if (existing == null) {
            throw new ServiceException("API Key 不存在");
        }
        if (form.getKeyName() != null) {
            existing.setKeyName(form.getKeyName());
        }
        if (form.getKeyDesc() != null) {
            existing.setKeyDesc(form.getKeyDesc());
        }
        if (form.getClientName() != null) {
            existing.setClientName(form.getClientName());
        }
        if (form.getScopeModules() != null) {
            existing.setScopeModules(form.getScopeModules());
        }
        if (form.getExpireTime() != null) {
            existing.setExpireTime(form.getExpireTime());
        }
        if (form.getBizAppId() != null) {
            existing.setBizAppId(form.getBizAppId());
        }
        existing.setUpdateTime(new Date());
        baseApiKeyMapper.updateById(existing);
        return existing;
    }

    @CacheEvict(value = JbmCacheConstants.API_KEY_CACHE_NAMESPACE, allEntries = true)
    @Override
    public String resetSecret(Long keyId) {
        BaseApiKey row = getByKeyId(keyId);
        if (row == null) {
            throw new ServiceException("API Key 不存在");
        }
        String plainSecret = RandomValueUtils.randomAlphanumeric(32);
        row.setSecretKey(SecurityUtils.encryptPassword(plainSecret));
        row.setUpdateTime(new Date());
        baseApiKeyMapper.updateById(row);
        return plainSecret;
    }

    @CacheEvict(value = JbmCacheConstants.API_KEY_CACHE_NAMESPACE, allEntries = true)
    @Override
    public void updateStatus(Long keyId, Integer status) {
        BaseApiKey row = getByKeyId(keyId);
        if (row == null) {
            throw new ServiceException("API Key 不存在");
        }
        row.setStatus(status);
        if (ApiKeyConstants.API_KEY_STATUS_DISABLED == status) {
            row.setRevokeTime(new Date());
        }
        row.setUpdateTime(new Date());
        baseApiKeyMapper.updateById(row);
    }

    @CacheEvict(value = JbmCacheConstants.API_KEY_CACHE_NAMESPACE, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void removeApiKey(Long keyId) {
        QueryWrapper<BaseAuthorityApikey> q = new QueryWrapper<>();
        q.lambda().eq(BaseAuthorityApikey::getKeyId, keyId);
        baseAuthorityApikeyMapper.delete(q);
        baseApiKeyMapper.deleteById(keyId);
    }

    @Override
    public List<OpenAuthority> findAuthorityByKeyId(Long keyId) {
        return baseAuthorityApikeyMapper.selectAuthorityByKeyId(keyId);
    }

    @CacheEvict(value = JbmCacheConstants.API_KEY_CACHE_NAMESPACE, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void grantAuthority(Long keyId, Long operatorUserId, Date expireTime, String... authorityIds) {
        BaseApiKey row = getByKeyId(keyId);
        if (row == null) {
            throw new ServiceException("API Key 不存在");
        }
        Set<String> allowed = loadGrantableAuthorityIds(operatorUserId);
        if (authorityIds != null) {
            for (String id : authorityIds) {
                if (!allowed.contains(id)) {
                    throw new ServiceException("无权授权该权限: authorityId=" + id);
                }
            }
        }
        QueryWrapper<BaseAuthorityApikey> q = new QueryWrapper<>();
        q.lambda().eq(BaseAuthorityApikey::getKeyId, keyId);
        baseAuthorityApikeyMapper.delete(q);
        if (authorityIds != null && authorityIds.length > 0) {
            Date now = new Date();
            for (String id : authorityIds) {
                BaseAuthorityApikey link = new BaseAuthorityApikey();
                link.setKeyId(keyId);
                link.setAuthorityId(Long.parseLong(id));
                link.setExpireTime(expireTime);
                link.setAuthStatus(ApiKeyConstants.AUTH_STATUS_ENABLED);
                link.setCreateTime(now);
                link.setUpdateTime(now);
                baseAuthorityApikeyMapper.insert(link);
            }
        }
    }

    @Override
    public boolean hasAuthority(Long keyId, Long authorityId) {
        if (keyId == null || authorityId == null) {
            return false;
        }
        BaseApiKey row = getByKeyId(keyId);
        if (row == null || row.getStatus() == null || row.getStatus() != ApiKeyConstants.API_KEY_STATUS_ENABLED) {
            return false;
        }
        if (row.getExpireTime() != null && row.getExpireTime().before(new Date())) {
            return false;
        }
        List<Long> ids = baseAuthorityApikeyMapper.selectAuthorityIdsByKeyId(keyId);
        return ids != null && ids.contains(authorityId);
    }

    @Override
    public boolean hasAuthorityForApi(Long keyId, Long apiId) {
        if (apiId == null) {
            return true;
        }
        BaseAuthority auth = baseAuthorityService.getAuthority(apiId, com.jbm.cluster.api.constants.ResourceType.api);
        if (auth == null || auth.getAuthorityId() == null) {
            return false;
        }
        return hasAuthority(keyId, auth.getAuthorityId());
    }

    @Override
    public void touchLastUsed(Long keyId) {
        BaseApiKey row = new BaseApiKey();
        row.setKeyId(keyId);
        row.setLastUsedTime(new Date());
        baseApiKeyMapper.updateById(row);
    }

    @Override
    public List<OpenAuthority> findGrantableAuthorities(Long operatorUserId) {
        Set<String> ids = loadGrantableAuthorityIds(operatorUserId);
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        boolean root = false;
        try {
            BaseUser user = baseUserService.getUserById(operatorUserId);
            root = user != null && JbmConstants.isSuperUser(user.getUserId(), user.getUserName(), user.getUserType());
        } catch (Exception ignored) {
        }
        List<OpenAuthority> authorities = baseAuthorityService.findAuthorityByUser(operatorUserId, root);
        if (authorities == null) {
            return Collections.emptyList();
        }
        return authorities.stream()
                .filter(a -> a.getAuthorityId() != null && ids.contains(a.getAuthorityId()))
                .collect(Collectors.toList());
    }

    private void assertDeveloperActive(Long userId) {
        if (userId == null) {
            throw new ServiceException("未登录");
        }
        BaseDeveloper dev = baseDeveloperService.getUserById(userId);
        if (dev == null || dev.getStatus() == null || dev.getStatus() != ApiKeyConstants.DEVELOPER_STATUS_ACTIVE) {
            throw new ServiceException("当前用户不是已审批通过的开发者");
        }
    }

    private Set<String> loadGrantableAuthorityIds(Long operatorUserId) {
        boolean root = false;
        try {
            BaseUser user = baseUserService.getUserById(operatorUserId);
            root = user != null && JbmConstants.isSuperUser(user.getUserId(), user.getUserName(), user.getUserType());
        } catch (Exception ignored) {
        }
        List<OpenAuthority> authorities = baseAuthorityService.findAuthorityByUser(operatorUserId, root);
        if (authorities == null) {
            return new HashSet<>();
        }
        return authorities.stream()
                .filter(a -> a.getAuthority() != null && a.getAuthority().startsWith(JbmSecurityConstants.AUTHORITY_PREFIX_API))
                .map(OpenAuthority::getAuthorityId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
    }

    private void ensureRsaKeyPair(BaseApiKey row) {
        if (ObjectUtil.hasEmpty(row.getPrivateKey(), row.getPublicKey())) {
            KeyPair keyPair = SecurityUtils.generateRSAKey(row.getSecretKey());
            RSA rsa = SecureUtil.rsa(keyPair.getPrivate().getEncoded(), keyPair.getPublic().getEncoded());
            row.setPrivateKey(rsa.getPrivateKeyBase64());
            row.setPublicKey(rsa.getPublicKeyBase64());
            baseApiKeyMapper.updateById(row);
        }
    }
}
