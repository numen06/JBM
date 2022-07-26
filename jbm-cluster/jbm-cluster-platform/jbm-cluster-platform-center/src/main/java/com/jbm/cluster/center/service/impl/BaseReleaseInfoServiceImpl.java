package com.jbm.cluster.center.service.impl;

import com.alibaba.fastjson.JSON;
import com.jbm.cluster.api.entitys.basic.BaseReleaseInfo;
import com.jbm.cluster.center.service.BaseReleaseInfoService;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @Author: wesley.zhang
 * @Create: 2021-08-25 10:49:30
 */
@Service
public class BaseReleaseInfoServiceImpl extends MasterDataServiceImpl<BaseReleaseInfo> implements BaseReleaseInfoService {

    @Override
    public BaseReleaseInfo findLastVersionInfo(BaseReleaseInfo releaseInfo) {
        Map<String, Object> map = this.selectMapperOne("findLastVersionInfo", releaseInfo);
        String json = JSON.toJSONString(map);
        releaseInfo = JSON.parseObject(json, BaseReleaseInfo.class);
        return releaseInfo;
    }


}