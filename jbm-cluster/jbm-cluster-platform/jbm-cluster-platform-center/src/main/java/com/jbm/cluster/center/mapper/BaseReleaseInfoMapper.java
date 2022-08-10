package com.jbm.cluster.center.mapper;

import com.jbm.cluster.api.entitys.basic.BaseReleaseInfo;
import com.jbm.framework.masterdata.annotation.MapperRepository;
import com.jbm.framework.masterdata.mapper.SuperMapper;

/**
 * @Author: wesley.zhang
 * @Create: 2021-08-25 10:49:30
 */
@MapperRepository
public interface BaseReleaseInfoMapper extends SuperMapper<BaseReleaseInfo> {

    BaseReleaseInfo findLastVersionInfo(BaseReleaseInfo releaseInfo);
}
