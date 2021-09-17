package com.jbm.framework.service.mybatis;


import com.jbm.framework.masterdata.mapper.SuperMapper;
import com.jbm.framework.masterdata.service.IMultiPlatformService;
import com.jbm.framework.masterdata.usage.entity.MultiPlatformEntity;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class MultiPlatformServiceImpl<Entity extends MultiPlatformEntity> extends MasterDataServiceImpl<Entity> implements IMultiPlatformService<Entity> {


}
