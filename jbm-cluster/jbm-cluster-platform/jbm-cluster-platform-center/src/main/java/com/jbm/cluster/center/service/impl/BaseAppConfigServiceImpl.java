package com.jbm.cluster.center.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.entitys.basic.BaseAppConfig;
import com.jbm.cluster.center.service.BaseAppConfigService;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: wesley.zhang
 * @Create: 2022-06-27 12:55:11
 */
@Service
public class BaseAppConfigServiceImpl extends MasterDataServiceImpl<BaseAppConfig> implements BaseAppConfigService {


    @Override
    public BaseAppConfig getAppConfigByKey(String appKey, Long orgId) {
        if (ObjectUtil.isEmpty(appKey)) {
            throw new RuntimeException("请求参数不能为空");
        }
        BaseAppConfig def = this.getAppDefConfigByKey(appKey);
        if (ObjectUtil.isEmpty(orgId)) {
            return def;
        }
        QueryWrapper<BaseAppConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BaseAppConfig::getAppKey, appKey)
                .eq(BaseAppConfig::getOrgId, orgId);
        List<BaseAppConfig> list = this.baseMapper.selectList(queryWrapper);
        return CollUtil.getFirst(list);
    }

    public BaseAppConfig getAppDefConfigByKey(String appKey) {
        QueryWrapper<BaseAppConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BaseAppConfig::getAppKey, appKey)
                .isNull(BaseAppConfig::getOrgId);
        List<BaseAppConfig> list = this.baseMapper.selectList(queryWrapper);
        if (CollUtil.isEmpty(list)) {
            throw new RuntimeException("当前APP找不到默认配置");
        }
        return CollUtil.getFirst(list);
    }

    @Override
    public BaseAppConfig saveEntity(BaseAppConfig baseAppConfig) {
        BaseAppConfig dbAppConfig = this.getAppDefConfigByKey(baseAppConfig.getAppKey());
        if (ObjectUtil.isNotEmpty(LoginHelper.softGetLoginUser())) {
            if (!LoginHelper.isAdmin()) {
                dbAppConfig = this.getAppConfigByKey(baseAppConfig.getAppKey(), LoginHelper.softGetLoginUser().getCompanyId());
            }
        }
        if (ObjectUtil.isNotEmpty(dbAppConfig)) {
            baseAppConfig.setId(dbAppConfig.getId());
        }
        return super.saveEntity(baseAppConfig);
    }
}