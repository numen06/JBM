package com.jbm.cluster.center.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.entitys.basic.BaseAppConfig;
import com.jbm.cluster.center.service.BaseAppConfigService;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: wesley.zhang
 * @Create: 2022-06-27 12:55:11
 */
@Service
public class BaseAppConfigServiceImpl extends MasterDataServiceImpl<BaseAppConfig> implements BaseAppConfigService {


    @Override
    @Cacheable(value = "appConfigByKey", key = "'appConfigByKey_'+#appKey+'_'+#orgId")
    public BaseAppConfig getAppConfigByKey(String appKey, Long orgId) {
        if (ObjectUtil.isEmpty(appKey)) {
            throw ServiceException.of("请求参数不能为空");
        }
        // 如果没有组织ID，则认为是默认配置
        if (ObjectUtil.isEmpty(orgId)) {
            return this.getAppDefConfigByKey(appKey);
        } else {
            QueryWrapper<BaseAppConfig> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(BaseAppConfig::getAppKey, appKey)
                    .eq(BaseAppConfig::getOrgId, orgId);
            List<BaseAppConfig> list = this.baseMapper.selectList(queryWrapper);
            // 如果没有查到，则认为是默认配置
            if (CollUtil.isEmpty(list)) {
                return this.getAppDefConfigByKey(appKey);
            }
            return CollUtil.getFirst(list);
        }
    }

    public BaseAppConfig getAppDefConfigByKey(String appKey) {
        QueryWrapper<BaseAppConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BaseAppConfig::getAppKey, appKey)
                .isNull(BaseAppConfig::getOrgId);
        List<BaseAppConfig> list = this.baseMapper.selectList(queryWrapper);
        // 如果没有查到，则认为是默认配置
        if (CollUtil.isEmpty(list)) {
            throw ServiceException.of("当前APP找不到默认配置，请通过管理员配置");
        }
        return CollUtil.getFirst(list);
    }

    @Override
    @CacheEvict(value = "appConfigByKey", allEntries = true)
    public BaseAppConfig saveEntity(BaseAppConfig baseAppConfig) {
        if (ObjectUtil.isNotEmpty(LoginHelper.softGetLoginUser())) {
            try {
                // 如果默认配置不存在则创建
                this.getAppDefConfigByKey(baseAppConfig.getAppKey());
            } catch (ServiceException e) {
                return super.saveEntity(baseAppConfig);
            }
            baseAppConfig.setOrgId(LoginHelper.getLoginUser().getCompanyId());
            BaseAppConfig dbAppConfig = this.getAppConfigByKey(baseAppConfig.getAppKey(), baseAppConfig.getOrgId());
            if (dbAppConfig.getOrgId() == null) {
                //用户是登录状态
                if (!LoginHelper.isAdmin()) {
                    baseAppConfig.setId(null);
                } else {
                    baseAppConfig.setOrgId(null);
                    baseAppConfig.setId(dbAppConfig.getId());
                }
            } else {
                baseAppConfig.setId(dbAppConfig.getId());
            }
        } else {
            BaseAppConfig dbAppConfig = this.getAppDefConfigByKey(baseAppConfig.getAppKey());
            baseAppConfig.setId(dbAppConfig.getId());
        }

        return super.saveEntity(baseAppConfig);
    }
}