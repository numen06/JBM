package com.jbm.cluster.center.service.impl;

import com.jbm.cluster.api.entitys.basic.BaseReleaseInfo;
import com.jbm.cluster.center.mapper.BaseReleaseInfoMapper;
import com.jbm.cluster.center.service.BaseReleaseInfoService;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author: wesley.zhang
 * @Create: 2021-08-25 10:49:30
 */
@Service
public class BaseReleaseInfoServiceImpl extends MasterDataServiceImpl<BaseReleaseInfo> implements BaseReleaseInfoService {


    @Autowired
    private BaseReleaseInfoMapper baseReleaseInfoMapper;

    @Override
    public BaseReleaseInfo findLastVersionInfo(BaseReleaseInfo releaseInfo) {
        BaseReleaseInfo baseReleaseInfo = baseReleaseInfoMapper.findLastVersionInfo(releaseInfo);
        return baseReleaseInfo;
    }


}