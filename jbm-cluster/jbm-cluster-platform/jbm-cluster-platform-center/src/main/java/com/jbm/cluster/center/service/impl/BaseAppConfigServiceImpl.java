package com.jbm.cluster.center.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.entitys.basic.BaseAppConfig;
import com.jbm.cluster.center.service.BaseAppConfigService;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-06-27 12:55:11
 */
@Service
public class BaseAppConfigServiceImpl extends MasterDataServiceImpl<BaseAppConfig> implements BaseAppConfigService {


    @Override
    public BaseAppConfig getAppConfigByKey(String appKey) {
        QueryWrapper<BaseAppConfig> queryWrapper = new QueryWrapper();
        queryWrapper.lambda().eq(BaseAppConfig::getAppKey, appKey);
        List<BaseAppConfig> list = this.baseMapper.selectList(queryWrapper);
        return CollUtil.getFirst(list);
    }
}