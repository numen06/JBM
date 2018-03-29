package com.td.framework.handler;

import java.io.Serializable;

import com.td.masterdata.entity.common.MasterLevelEntity;

public interface ILevelSqlHandlerService<Entity extends MasterLevelEntity<CODE>, CODE extends Serializable> extends IAdvSqlHandlerService<Entity> {
}
