package com.jbm.framework.service.mybatis;

import java.io.Serializable;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.jbm.framework.metadata.usage.bean.AdvEntity;

/**
 * 
 * 高级Mongo基础服务
 * 
 * @author wesley
 *
 * @param <RESP>
 * @param <Entity>
 * @param <PK>
 */
public class AdvSqlDaoImpl<Mapper extends BaseMapper<Entity>, Entity extends AdvEntity<CODE>, CODE extends Serializable>
		extends BaseSqlDaoImpl<Entity, Long> {

}
