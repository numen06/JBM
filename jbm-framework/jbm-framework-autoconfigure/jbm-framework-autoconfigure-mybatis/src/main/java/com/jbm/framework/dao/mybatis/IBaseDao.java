package com.jbm.framework.dao.mybatis;

import java.io.Serializable;
import java.util.Map;

import com.jbm.framework.metadata.exceptions.DataServiceException;
import com.jbm.framework.metadata.usage.page.DataPaging;
import com.jbm.framework.metadata.usage.page.PageForm;

/**
 * 
 * 基础数据库处理层的接口
 * 
 * @author Wesley
 * 
 * @param <Entity>
 * @param <PK>
 */
public interface IBaseDao<Entity extends Serializable, PK extends Serializable> {


}
