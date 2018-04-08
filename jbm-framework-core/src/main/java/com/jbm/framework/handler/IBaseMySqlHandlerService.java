package com.jbm.framework.handler;

import java.io.Serializable;

import com.jbm.framework.service.IBaseMySqlService;

public interface IBaseMySqlHandlerService<Entity extends Serializable> extends IBaseHandlerService<Entity>, IBaseMySqlService<Entity> {

}
