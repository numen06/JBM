package com.jbm.framework.handler;

import java.io.Serializable;

import com.jbm.framework.service.IBaseMongoService;

public interface IBaseMongoHandlerService<Entity extends Serializable> extends IBaseMongoService<Entity> {

}
