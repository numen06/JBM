package com.jbm.framework.mvc.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.jbm.framework.form.JsonRequestBody;
import com.jbm.framework.metadata.bean.ResultForm;
import com.jbm.framework.metadata.exceptions.ServiceException;
import com.jbm.framework.metadata.usage.bean.BaseEntity;
import com.jbm.framework.metadata.usage.page.DataPaging;
import com.jbm.framework.metadata.usage.page.PageForm;
import com.jbm.framework.service.IBaseSqlService;
import com.jbm.util.ClassUtils;
import com.jbm.util.StringUtils;
import com.xiaoleilu.hutool.lang.Validator;

public abstract class ManagerCollection<Entity extends BaseEntity, Service extends IBaseSqlService<Entity, Long>> {
	@Autowired
	protected Service service;
	@Autowired
	protected MessageSource messageSource;

	protected Class<Entity> entityClass;

	@SuppressWarnings("unchecked")
	public ManagerCollection() {
		super();
		entityClass = (Class<Entity>) ClassUtils.getSuperClassGenricType(this.getClass(), 0);
	}

	private String getSimpleClassName() {
		return StringUtils.left(entityClass.getSimpleName(), 1).toLowerCase() + StringUtils.substring(entityClass.getSimpleName(), 1);
	}

	@RequestMapping("/pageList")
	public Object pageList(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		try {
			if (Validator.isNull(jsonRequestBody)) {
				throw new ServiceException("参数错误");
			}
			Entity entity = jsonRequestBody.getObject(getSimpleClassName(), entityClass);
			PageForm pageForm = jsonRequestBody.getPageForm();
			DataPaging<Entity> dataPaging = service.selectEntitys(entity, pageForm);
			return ResultForm.createSuccessResultForm(dataPaging, "成功");
		} catch (Exception e) {
			return ResultForm.createErrorResultForm(null, "查询失败");
		}
	}

	@RequestMapping("/model")
	public Object model(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		try {
			if (Validator.isNull(jsonRequestBody)) {
				throw new ServiceException("参数错误");
			}
			Entity entity = jsonRequestBody.getObject(getSimpleClassName(), entityClass);
			if (Validator.isNull(entity)) {
				throw new ServiceException("参数错误");
			}
			entity = service.selectEntity(entity);
			return ResultForm.createSuccessResultForm(entity, "成功");
		} catch (Exception e) {
			return ResultForm.createErrorResultForm(null, "查询失败");
		}
	}

	@RequestMapping("/save")
	public Object save(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		try {
			if (Validator.isNull(jsonRequestBody)) {
				throw new ServiceException("参数错误");
			}
			Entity entity = jsonRequestBody.getObject(getSimpleClassName(), entityClass);
			if (Validator.isNull(entity)) {
				throw new ServiceException("参数错误");
			}
			entity = service.save(entity);
			return ResultForm.createSuccessResultForm(entity, "成功");
		} catch (Exception e) {
			return ResultForm.createErrorResultForm(null, "查询失败");
		}
	}

	@RequestMapping("/delete")
	public Object remove(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		try {
			if (Validator.isNull(jsonRequestBody)) {
				throw new ServiceException("参数错误");
			}
			final Entity entity = jsonRequestBody.getObject(getSimpleClassName(), entityClass);
			if (Validator.isNull(entity)) {
				throw new ServiceException("参数错误");
			}
			service.delete(entity);
			// smsConfService.delete(smsConf);
			return ResultForm.createSuccessResultForm(entity, "成功");
		} catch (Exception e) {
			System.err.println(e);
			return ResultForm.createErrorResultForm(null, "删除失败");
		}
	}
}
