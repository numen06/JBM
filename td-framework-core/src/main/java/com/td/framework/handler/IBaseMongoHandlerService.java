package com.td.framework.handler;

import java.io.Serializable;

import com.td.framework.service.IBaseMongoService;

public interface IBaseMongoHandlerService<Entity extends Serializable> extends IBaseMongoService<Entity> {

}
