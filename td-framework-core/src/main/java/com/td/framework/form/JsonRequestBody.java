package com.td.framework.form;

import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.td.framework.bo.usage.AbstractBizParam;
import com.td.framework.metadata.usage.page.PageForm;
import com.td.util.StringUtils;

public class JsonRequestBody extends JSONObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private AbstractBizParam bizParam;

	private PageForm pageForm;

	public JsonRequestBody() {
		super();
	}

	@Override
	public Object put(String key, Object value) {
		return super.put(key, value);
	}

	/**
	 * 默认获取同类名字<br/>
	 * 段如果没有会在ROOT节点下查找
	 * 
	 * @param clazz
	 * @return
	 */
	public <T> T tryGet(Class<T> clazz) {
		return tryGet(StringUtils.uncapitalize(clazz.getSimpleName()), clazz);
	}

	public <T> List<T> tryGetList(Class<T> clazz) {
		return JSON.parseArray(this.toJSONString(), clazz);
	}

	public <T> List<T> getList(String key, Class<T> clazz) {
		return JSON.parseArray(this.getJSONArray(key).toJSONString(), clazz);
	}

	/**
	 * 在文本中获取相关字段
	 * 
	 * @param name
	 *            字段名称
	 * @param clazz
	 *            转换类类型
	 * @return
	 */
	public <T> T tryGet(String name, Class<T> clazz) {
		if (StringUtils.isNotBlank(name) && this.containsKey(name)) {
			return JSON.toJavaObject(this.getJSONObject(name), clazz);
		}
		return JSON.toJavaObject(this, clazz);
	}

	public Map<String, Object> tryToMap() {
		return this;
	}

	public PageForm getPageForm() {
		if (pageForm == null)
			pageForm = this.tryGet("pageForm", PageForm.class);
		return pageForm;
	}

	public AbstractBizParam getBizParam() {
		if (bizParam == null)
			bizParam = this.tryGet("bizParam", AbstractBizParam.class);
		return bizParam;
	}

}
