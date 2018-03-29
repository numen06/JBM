package com.td.framework.handler;

import java.io.Serializable;

/**
 * 
 * 高级服务接口,支持主键
 * 
 * @author Wesley
 * 
 * @param <Entity>
 * @param <PK>
 */
public interface IAdvHandlerService<Entity extends Serializable, PK extends Serializable> extends IBaseHandlerService<Entity> {

}
