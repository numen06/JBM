package com.td.framework.handler;

import java.io.Serializable;

import com.td.framework.service.IBaseMySqlService;

public interface IBaseMySqlHandlerService<Entity extends Serializable> extends IBaseHandlerService<Entity>, IBaseMySqlService<Entity> {

}
