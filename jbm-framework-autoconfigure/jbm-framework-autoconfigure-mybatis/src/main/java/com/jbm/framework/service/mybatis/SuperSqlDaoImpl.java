package com.jbm.framework.service.mybatis;

import java.io.Serializable;

import com.jbm.framework.metadata.usage.bean.AdvEntity;

public class SuperSqlDaoImpl<Entity extends AdvEntity<CODE>, CODE extends Serializable> extends BaseSqlDaoImpl<Entity, Long> {

}
