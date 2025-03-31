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
        QueryWrapper<BaseAppConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BaseAppConfig::getAppKey, appKey)
                .eq(orgId != null, BaseAppConfig::getOrgId, orgId)
                .isNull(orgId == null, BaseAppConfig::getOrgId);
        List<BaseAppConfig> list = this.baseMapper.selectList(queryWrapper);
        return CollUtil.getFirst(list);
    }

    @Override
    public BaseAppConfig saveEntity(BaseAppConfig baseAppConfig) {
        BaseAppConfig dbAppConfig = this.getAppConfigByKey(baseAppConfig.getAppKey(), null);
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