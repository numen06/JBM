package com.jbm.web.controller;

import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.jbm.web.support.DateEditor;
import com.jbm.web.support.SqlDateEditor;
import com.jbm.web.support.SqlTimestampEditor;

public abstract class BaseController {
	private static final Logger logger = LoggerFactory.getLogger(BaseController.class);

	/**
	 * 注册转换类型
	 * 
	 * @param request
	 * @param binder
	 * @throws Exception
	 */
	@InitBinder
	protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws Exception {
		// 对于需要转换为Date类型的属性，使用DateEditor进行处理
		binder.registerCustomEditor(java.sql.Date.class, new SqlDateEditor());
		binder.registerCustomEditor(java.sql.Timestamp.class, new SqlTimestampEditor());
		binder.registerCustomEditor(java.util.Date.class, new DateEditor());
	}

	/**
	 * 统一异常处理
	 * 
	 * @param request
	 * @param ex
	 * @return
	 */
	@ExceptionHandler
	public final ModelAndView exception(HttpServletRequest request, Exception ex) {
		logger.error("前端拦截异常", ex);
		// 添加自己的异常处理逻辑，如日志记录
		request.setAttribute("exceptionMessage", ex.getMessage());
		// 根据不同的异常类型进行不同处理
		if (ex instanceof SQLException)
			return new ModelAndView("error");
		else
			return new ModelAndView("error");
	}

	/**
	 * 将对象转换成JSON字符串
	 * 
	 * @param obj
	 * @return
	 */
	public String serializerJson(Object obj) {
		return JSON.toJSONString(obj, SerializerFeature.DisableCircularReferenceDetect);
	}

}
