package com.jbm.framework.service.mybatis;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Sort.Direction;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.plugins.pagination.Pagination;
import com.jbm.framework.metadata.usage.page.PageForm;
import com.jbm.util.MapUtils;
import com.jbm.util.StringUtils;

public class SelectMapperParams<Entity> {

	public SelectMapperParams(Entity et) {
		super();
		this.et = et;
		this.ew = new EntityWrapper<>(et);
	}

	@SuppressWarnings("unchecked")
	public <K, V> SelectMapperParams(Map<K, V> cm) {
		super();
		this.cm = (Map<String, Object>) cm;
	}

	public SelectMapperParams(Wrapper<Entity> ew) {
		super();
		this.ew = ew;
		this.et = ew.getEntity();
	}

	public SelectMapperParams(Entity et, Wrapper<Entity> ew) {
		super();
		this.ew = ew;
		this.et = et;
	}

	public SelectMapperParams(Entity et, PageForm pageForm) {
		this(et);
		this.pageForm = pageForm;
		this.toPagination(pageForm);
	}

	public <K, V> SelectMapperParams(Map<K, V> cm, PageForm pageForm) {
		this(cm);
		this.pageForm = pageForm;
		this.toPagination(pageForm);
	}

	public SelectMapperParams(Page<Entity> pagination, Wrapper<Entity> ew) {
		super();
		this.pagination = pagination;
		this.ew = ew;
	}

	public PageForm pageForm;

	public Page<Entity> pagination;

	private Wrapper<Entity> ew;

	private Map<String, Object> cm;

	private Entity et;

	public Wrapper<Entity> getEw() {
		return ew;
	}

	public void setEw(Wrapper<Entity> ew) {
		this.ew = ew;
	}

	public Entity getEt() {
		return et;
	}

	public void setEt(Entity et) {
		this.et = et;
	}

	public Map<String, Object> getCm() {
		return cm;
	}

	public void setCm(Map<String, Object> cm) {
		this.cm = cm;
	}

	public PageForm getPageForm() {
		return pageForm;
	}

	public void setPageForm(PageForm pageForm) {
		this.pageForm = pageForm;
	}

	public Page<Entity> getPagination() {
		return pagination;
	}

	private Pagination toPagination(PageForm pageForm) {
		this.pagination = new Page<Entity>(pageForm.getCurrPage(), pageForm.getPageSize());
		Map<String, String> rules = MapUtils.split(pageForm.getSortRule(), new HashMap<String, String>(), ",", ":");
		for (String key : rules.keySet()) {
			final String column = StringUtils.trimToEmpty(key);
			final Boolean isAsc = Direction.fromStringOrNull(StringUtils.trimToEmpty(rules.get(key))).isAscending();
			this.getEw().orderBy(column, isAsc);
		}
		return pagination;
	}
}
