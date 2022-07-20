package com.jbm.cluster.center.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.entitys.basic.BaseUserCertification;
import com.jbm.cluster.center.service.BaseUserCertificationService;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-07-19 14:01:27
 */
@Service
public class BaseUserCertificationServiceImpl extends MasterDataServiceImpl<BaseUserCertification> implements BaseUserCertificationService {


    @Override
    public BaseUserCertification findByUserId(Long userId) {
        QueryWrapper<BaseUserCertification> queryWrapper = new QueryWrapper();
        queryWrapper.lambda()
                .eq(BaseUserCertification::getUserId, userId);
        return this.selectEntityByWapper(queryWrapper);
    }

}