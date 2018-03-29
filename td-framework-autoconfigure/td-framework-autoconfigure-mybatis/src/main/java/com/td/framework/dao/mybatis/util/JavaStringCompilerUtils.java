package com.td.framework.dao.mybatis.util;

import java.text.MessageFormat;
import java.util.Map;

import com.itranswarp.compiler.JavaStringCompiler;

/**
 * @author wesley.zhang
 * @date 2017年11月27日
 * @version 1.0
 *
 */
public class JavaStringCompilerUtils {
	private final static JavaStringCompiler compiler = new JavaStringCompiler();

	public static Class<?> compilerMapper(final Class<?> entityClazz, final String clazzTemplate) throws Exception {
		String mapperClass;
		Map<String, byte[]> results;
		String packages = "com.td.sample.mysql.resp";
		String className = entityClazz.getSimpleName() + "Mapper";
		String entity = entityClazz.getName();
		mapperClass = packages + "." + className;
		String source = MessageFormat.format(clazzTemplate, packages, className, entity);
		results = compiler.compile(className + ".java", source);
		Class<?> mapper = compiler.loadClass(mapperClass, results);
		return mapper;
	}
}
