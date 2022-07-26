package com.jbm.cluster.center.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.cluster.api.entitys.basic.BaseDeveloper;
import com.jbm.cluster.center.mapper.BaseAppMapper;
import com.jbm.cluster.center.service.BaseAppService;
import com.jbm.cluster.center.service.BaseAuthorityService;
import com.jbm.cluster.core.constant.JbmCacheConstants;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.masterdata.usage.CriteriaQueryWrapper;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.util.RandomValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @author: wesley.zhang
 * @date: 2018/11/12 16:26
 * @description:
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class BaseAppServiceImpl extends MasterDataServiceImpl<BaseApp> implements BaseAppService {

    /**
     * token有效期，默认12小时
     */
    public static final int ACCESS_TOKEN_VALIDITY_SECONDS = 60 * 60 * 12;
    /**
     * token有效期，默认7天
     */
    public static final int REFRESH_TOKEN_VALIDITY_SECONDS = 60 * 60 * 24 * 7;
//    @Autowired
//    private JdbcClientDetailsService jdbcClientDetailsService;
    @Autowired
    private BaseAppMapper baseAppMapper;
    @Autowired
    private BaseAuthorityService baseAuthorityService;

    @Override
    public BaseApp saveEntity(BaseApp entity) {
        if (StrUtil.isBlank(entity.getAppId())) {
            return this.addAppInfo(entity);
        } else {
            return this.updateInfo(entity);
        }
//        return super.saveEntity(entity);
    }

    /**
     * 查询应用列表
     *
     * @param pageRequestBody
     * @return
     */
    @Override
    public DataPaging<BaseApp> findListPage(PageRequestBody pageRequestBody) {
        BaseApp query = pageRequestBody.tryGet(BaseApp.class);
        CriteriaQueryWrapper<BaseApp> cq = CriteriaQueryWrapper.from(pageRequestBody.getPageParams());
        cq.lambda()
                .eq(ObjectUtils.isNotEmpty(query.getDeveloperId()), BaseApp::getDeveloperId, query.getDeveloperId())
                .eq(ObjectUtils.isNotEmpty(query.getAppType()), BaseApp::getAppType, query.getAppType())
                .eq(ObjectUtils.isNotEmpty(pageRequestBody.get("aid")), BaseApp::getAppId, pageRequestBody.get("aid"))
                .likeRight(ObjectUtils.isNotEmpty(query.getAppName()), BaseApp::getAppName, query.getAppName())
                .likeRight(ObjectUtils.isNotEmpty(query.getAppNameEn()), BaseApp::getAppNameEn, query.getAppNameEn());
        cq.select("app.*,developer.user_name");
        //关联BaseDeveloper表
        cq.createAlias(BaseDeveloper.class);
        cq.orderByDesc("create_time");
        return this.selectEntitys(cq);
    }

    /**
     * 获取app详情
     *
     * @param appId
     * @return
     */
    @Cacheable(value = JbmCacheConstants.APP_CACHE_NAMESPACE, key = "#appId")
    @Override
    public BaseApp getAppInfo(String appId) {
        return baseAppMapper.selectById(appId);
    }

    @Cacheable(value = JbmCacheConstants.APP_CACHE_NAMESPACE, key = "#appKey")
    @Override
    public BaseApp getAppInfoByKey(String appKey) {
        if (ObjectUtils.isEmpty(appKey)) {
            throw new ServiceException("key为空");
        }
        QueryWrapper<BaseApp> queryWrapper = new QueryWrapper();
        queryWrapper.lambda().eq(BaseApp::getApiKey, appKey);
        List<BaseApp> list = baseAppMapper.selectList(queryWrapper);
        return CollUtil.getFirst(list);
    }

    /**
     * 获取app和应用信息
     *
     * @return
     */
//    @Override
//    @Cacheable(value = JbmCacheConstants.APP_CACHE_NAMESPACE, key = "'client:'+#clientId")
//    public OpenClientDetails getAppClientInfo(String clientId) {
////        BaseClientDetails baseClientDetails = null;
////        try {
////            baseClientDetails = (BaseClientDetails) jdbcClientDetailsService.loadClientByClientId(clientId);
////        } catch (Exception e) {
////            return null;
////        }
////        String appId = baseClientDetails.getAdditionalInformation().get("appId").toString();
//        OpenClientDetails openClient = new OpenClientDetails();
//        BeanUtils.copyProperties(baseClientDetails, openClient);
//        openClient.setAuthorities(baseAuthorityService.findAuthorityByApp(appId));
//        return openClient;
//    }

//    /**
//     * 更新应用开发新型
//     *
//     * @param client
//     */
//    @CacheEvict(value = {"apps"}, key = "'client:'+#client.clientId")
//    @Override
//    public void updateAppClientInfo(OpenClientDetails client) {
//        jdbcClientDetailsService.updateClientDetails(client);
//    }

    /**
     * 添加应用
     *
     * @param app
     * @return 应用信息
     */
    @CachePut(value = "apps", key = "#app.appId")
    @Override
    public BaseApp addAppInfo(BaseApp app) {
        String appId = String.valueOf(System.currentTimeMillis());
        String apiKey = RandomValueUtils.randomAlphanumeric(24);
        String secretKey = RandomValueUtils.randomAlphanumeric(32);
        app.setAppId(appId);
        app.setApiKey(apiKey);
        app.setSecretKey(secretKey);
        app.setCreateTime(new Date());
        app.setUpdateTime(app.getCreateTime());
        if (app.getIsPersist() == null) {
            app.setIsPersist(0);
        }
        baseAppMapper.insert(app);
//        Map info = BeanUtil.beanToMap(app);
        // 功能授权
//        BaseClientDetails client = new BaseClientDetails();
//        client.setClientId(app.getApiKey());
//        client.setClientSecret(app.getSecretKey());
//        client.setAdditionalInformation(info);
//        client.setAuthorizedGrantTypes(Arrays.asList("authorization_code", "client_credentials", "implicit", "refresh_token"));
//        client.setAccessTokenValiditySeconds(ACCESS_TOKEN_VALIDITY_SECONDS);
//        client.setRefreshTokenValiditySeconds(REFRESH_TOKEN_VALIDITY_SECONDS);
//        jdbcClientDetailsService.addClientDetails(client);
        return app;
    }

    /**
     * 修改应用
     *
     * @param app 应用
     * @return 应用信息
     */
    @Caching(evict = {
            @CacheEvict(value = {"apps"}, key = "#app.appId"),
            @CacheEvict(value = {"apps"}, key = "'client:'+#app.appId")
    })
    @Override
    public BaseApp updateInfo(BaseApp app) {
        app.setUpdateTime(new Date());
        baseAppMapper.updateById(app);
        // 修改客户端附加信息
//        BaseApp appInfo = getAppInfo(app.getAppId());
//        Map info = BeanUtil.beanToMap(appInfo);
//        BaseClientDetails client = (BaseClientDetails) jdbcClientDetailsService.loadClientByClientId(appInfo.getApiKey());
//        client.setAdditionalInformation(info);
//        jdbcClientDetailsService.updateClientDetails(client);
        return app;
    }

    /**
     * 重置秘钥
     *
     * @param appId
     * @return
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = {"apps"}, key = "#appId"),
            @CacheEvict(value = {"apps"}, key = "'client:'+#appId")
    })
    public String restSecret(String appId) {
        BaseApp appInfo = getAppInfo(appId);
        if (appInfo == null) {
            throw new ServiceException(appId + "应用不存在!");
        }
        if (appInfo.getIsPersist().equals(JbmConstants.ENABLED)) {
            throw new ServiceException(String.format("保留数据,不允许修改"));
        }
        // 生成新的密钥
        String secretKey = RandomValueUtils.randomAlphanumeric(32);
        appInfo.setSecretKey(secretKey);
        appInfo.setUpdateTime(new Date());
        baseAppMapper.updateById(appInfo);
//        jdbcClientDetailsService.updateClientSecret(appInfo.getApiKey(), secretKey);
        return secretKey;
    }

    /**
     * 删除应用
     *
     * @param appId
     * @return
     */
    @Caching(evict = {
            @CacheEvict(value = {"apps"}, key = "#appId"),
            @CacheEvict(value = {"apps"}, key = "'client:'+#appId")
    })
    @Override
    public void removeApp(String appId) {
        BaseApp appInfo = getAppInfo(appId);
        if (appInfo == null) {
            throw new ServiceException(appId + "应用不存在!");
        }
        if (appInfo.getIsPersist().equals(JbmConstants.ENABLED)) {
            throw new ServiceException(String.format("保留数据,不允许删除"));
        }
        // 移除应用权限
        baseAuthorityService.removeAuthorityApp(appId);
        baseAppMapper.deleteById(appInfo.getAppId());
//        jdbcClientDetailsService.removeClientDetails(appInfo.getApiKey());
    }

//    public static void main(String[] args) {
//        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
//        String apiKey = String.valueOf(RandomValueUtils.randomAlphanumeric(24));
//        String secretKey = String.valueOf(RandomValueUtils.randomAlphanumeric(32));
//        System.out.println("apiKey=" + apiKey);
//        System.out.println("secretKey=" + secretKey);
//        System.out.println("encodeSecretKey=" + encoder.encode(secretKey));
//    }

}
