package com.jbm.framework.service.mybatis;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.jbm.framework.metadata.exceptions.DataServiceException;
import com.jbm.framework.service.ILevelSqlService;
import com.jbm.masterdata.entity.common.MasterLevelEntity;
import com.jbm.util.CollectionUtils;

public class LevelSqlDaoImpl<Entity extends MasterLevelEntity<CODE>, CODE extends Serializable>
		extends BaseSqlDaoImpl<Entity, Long> implements ILevelSqlService<Entity, CODE> {

	public List<Entity> selectRootCodeList(Entity entity) throws DataServiceException {
		EntityWrapper<Entity> entityWrapper = new EntityWrapper<>(entity);
		entityWrapper.isNull("parent_code");
		return this.selectList(entityWrapper);
	}

	public List<Entity> selectRootIdList(Entity entity) throws DataServiceException {
		EntityWrapper<Entity> entityWrapper = new EntityWrapper<>(entity);
		entityWrapper.isNull("parent_id");
		return this.selectList(entityWrapper);
	}

	public List<Entity> selectTreeByParentCode(Entity entity) throws DataServiceException {
		CODE parentCode = entity.getParentCode();
		List<Entity> subEntitys = new ArrayList<Entity>();
		if (parentCode == null) {
			subEntitys = this.selectRootCodeList(entity);
		} else {
			subEntitys = this.selectListByParentCode(entity.getParentCode());
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
		EntityWrapper<Entity> entityWrapper = new EntityWrapper<>(null);
		entityWrapper.eq("parent_code", parentCode);
		return this.selectList(entityWrapper);
	}

	@Override
	public List<Entity> selectTreeByParentId(Entity entity) throws DataServiceException {
		CODE parentCode = entity.getParentCode();
		List<Entity> subEntitys = new ArrayList<Entity>();
		if (parentCode == null) {
			subEntitys = this.selectRootIdList(entity);
		} else {
			subEntitys = this.selectListByParentId(entity.getId());
		}
		return this.selectTreeByParentCode(subEntitys);
	}

	@Override
	public List<Entity> selectListByParentId(Long id) throws DataServiceException {
		EntityWrapper<Entity> entityWrappesr = new EntityWrapper<>(null);
		entityWrappesr.eq("parent_id", id);
		System.out.println(entityWrappesr.getSqlSegment());
		return this.selectList(entityWrappesr);
	}
}
