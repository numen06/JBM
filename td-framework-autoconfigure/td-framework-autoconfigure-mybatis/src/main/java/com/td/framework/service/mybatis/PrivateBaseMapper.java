package com.td.framework.service.mybatis;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.RowBounds;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.mapper.Wrapper;

public final class PrivateBaseMapper<T> implements BaseMapper<T> {

	@Override
	public Integer insert(T entity) {
		return null;
	}

	@Override
	public Integer insertAllColumn(T entity) {

		return null;
	}

	@Override
	public Integer deleteById(Serializable id) {

		return null;
	}

	@Override
	public Integer deleteByMap(Map<String, Object> columnMap) {

		return null;
	}

	@Override
	public Integer delete(Wrapper<T> wrapper) {

		return null;
	}

	@Override
	public Integer deleteBatchIds(List<? extends Serializable> idList) {

		return null;
	}

	@Override
	public Integer updateById(T entity) {

		return null;
	}

	@Override
	public Integer updateAllColumnById(T entity) {

		return null;
	}

	@Override
	public Integer update(T entity, Wrapper<T> wrapper) {

		return null;
	}

	@Override
	public T selectById(Serializable id) {

		return null;
	}

	@Override
	public List<T> selectBatchIds(List<? extends Serializable> idList) {

		return null;
	}

	@Override
	public List<T> selectByMap(Map<String, Object> columnMap) {

		return null;
	}

	@Override
	public T selectOne(T entity) {

		return null;
	}

	@Override
	public Integer selectCount(Wrapper<T> wrapper) {

		return null;
	}

	@Override
	public List<T> selectList(Wrapper<T> wrapper) {

		return null;
	}

	@Override
	public List<Map<String, Object>> selectMaps(Wrapper<T> wrapper) {

		return null;
	}

	@Override
	public List<Object> selectObjs(Wrapper<T> wrapper) {

		return null;
	}

	@Override
	public List<T> selectPage(RowBounds rowBounds, Wrapper<T> wrapper) {

		return null;
	}

	@Override
	public List<Map<String, Object>> selectMapsPage(RowBounds rowBounds, Wrapper<T> wrapper) {

		return null;
	}

}
