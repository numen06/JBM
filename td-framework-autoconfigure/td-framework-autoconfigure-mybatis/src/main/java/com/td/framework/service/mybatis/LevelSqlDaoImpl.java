package com.td.framework.service.mybatis;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.td.framework.metadata.exceptions.DataServiceException;
import com.td.masterdata.entity.common.MasterLevelEntity;
import com.td.util.CollectionUtils;
import com.td.util.MapUtils;

public class LevelSqlDaoImpl<Entity extends MasterLevelEntity<CODE>, CODE extends Serializable> extends BaseSqlDaoImpl<Entity, Long> {

	public List<Entity> selectRootList(Entity entity) throws DataServiceException {
		EntityWrapper<Entity> entityWrapper = new EntityWrapper<>(entity);
		entityWrapper.isNotNull("parent_code");
		return this.selectList(entityWrapper);
	}

	public List<Entity> selectTreeByParentCode(Entity entity) throws DataServiceException {
		CODE parentCode = entity.getParentCode();
		List<Entity> subEntitys = new ArrayList<Entity>();
		if (parentCode == null) {
			subEntitys = this.selectRootList(entity);
		} else {
			subEntitys = this.selectEntitys(MapUtils.newParamMap("parentCode", entity.getParentCode()));
		}
		return this.selectTreeByParentCode(subEntitys);
	}

	public List<Entity> selectTreeByParentCode(List<Entity> subEntitys) throws DataServiceException {
		if (CollectionUtils.isEmpty(subEntitys)) {
			subEntitys = new ArrayList<Entity>();
		}
		for (Iterator<Entity> iterator = subEntitys.iterator(); iterator.hasNext();) {
			Entity subEntity = iterator.next();
			this.selectTreeByParentCode(subEntity);
		}
		return subEntitys;
	}

	public List<Entity> selectListByParentCode(CODE parentCode) throws DataServiceException {
		return this.selectEntitys(MapUtils.newParamMap("parentCode", parentCode));
	}
}
