package com.td.framework.handler;

import java.io.Serializable;

import com.td.framework.service.IAdvMySqlService;

public interface IAdvMySqlHandlerService<Entity extends Serializable, PK extends Serializable> extends IAdvHandlerService<Entity, PK>,IAdvMySqlService<Entity, PK> {

}
