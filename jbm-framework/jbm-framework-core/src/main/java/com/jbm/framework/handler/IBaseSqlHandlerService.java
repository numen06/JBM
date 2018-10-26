package com.jbm.framework.handler;

import com.jbm.framework.metadata.usage.bean.BaseEntity;
import com.jbm.framework.service.IBaseSqlService;

public interface IBaseSqlHandlerService<Entity extends BaseEntity> extends IBaseSqlService<Entity, Long> {

}
