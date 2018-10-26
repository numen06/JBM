package com.jbm.framework.handler;

import java.io.Serializable;

import com.jbm.masterdata.entity.common.MasterLevelEntity;

public interface ILevelSqlHandlerService<Entity extends MasterLevelEntity<CODE>, CODE extends Serializable> extends IAdvSqlHandlerService<Entity> {
}
