package com.td.framework.handler;

import com.td.framework.metadata.usage.bean.BaseEntity;
import com.td.framework.service.IBaseSqlService;

public interface IBaseSqlHandlerService<Entity extends BaseEntity> extends IBaseSqlService<Entity, Long> {

}
