package com.td.framework.dao.mybatis.util;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ClassUtils;

import com.td.util.TemplateUtils;

import jodd.io.FileNameUtil;
import jodd.io.FileUtil;

public class SourceReverseTools {
	public static class SourcePathInfo {

		private SouceType souceType;
		private String basePackage;
		private Class<?> entityClass;

		public SourcePathInfo(String basePackage, Class<?> entityClass, SourceReverseTools.SouceType souceType) {
			super();
			this.souceType = souceType;
			this.basePackage = basePackage;
			this.entityClass = entityClass;
		}

		public String getClassName() {
			String fileName = MessageFormat.format("{0}{1}.{2}", entityClass.getSimpleName(), this.souceType.getType(), "java");
			return fileName;
		}

		public String getPackageName() {
			String pkName = MessageFormat.format("{0}.{1}", basePackage, this.souceType.getPacket());
			return pkName;
		}

		public File getPackagePath() throws IOException {
			String pkName = StringUtils.replace(this.getPackageName(), ".", "/");
			ClassPathResource temp = new ClassPathResource("/");
			String url = StringUtils.replace(temp.getURI().getPath(), "target/classes/", "src/main/java/");
			url = StringUtils.replace(temp.getURI().getPath(), "target/test-classes/", "src/main/java/");
			File pk = new File(url + pkName);
			return pk;
		}

		public File getClassPath() throws IOException {
			File pk = this.getPackagePath();
			String fileName = this.getClassName();
			String file = FileNameUtil.concat(pk.getPath(), fileName);
			return new File(file);
		}

	}

	public enum SouceType {

		SERVICE("Service", "service"), SERVICE_IMPL("ServiceImpl", "service.impl"), MAPPER("Mapper", "mapper");
		private final String type;

		private final String packet;

		private SouceType(String type, String packet) {
			this.type = type;
			this.packet = packet;
		}

		public String getType() {
			return type;
		}

		public String getPacket() {
			return packet;
		}

	}

	private static <T extends Serializable> Map<String, Object> buildParam(Class<T> entityClass, String basePackage) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("entityClass", entityClass);
		map.put("basePackage", basePackage);
		return map;
	}

	private static String render(String path, Map<String, Object> map) {
		try {
			System.out.println("----------start----------");
			ClassPathResource temp = new ClassPathResource(path);
			String out = TemplateUtils.renderTemplate(temp.getInputStream(), map);
			System.out.println(out);
			System.out.println("----------end----------");
			return out;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static <T extends Serializable> void write(SourcePathInfo sourcePathInfo, String data) {
		try {
			File packagePath = sourcePathInfo.getPackagePath();
			if (!packagePath.exists()) {
				FileUtils.forceMkdir(packagePath);
			}
			File classPath = sourcePathInfo.getClassPath();
			System.out.println(classPath);
			FileUtil.writeString(classPath, data, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static <T> void scanningWrite(Class<T> entityClass, String basePackage) {
		String classPath = ClassUtils.convertClassNameToResourcePath(entityClass.getName());
		System.out.println(classPath);
	}

	public static <T extends Serializable> void writeAll(Class<T> entityClass, String basePackage) {
		writeService(entityClass, basePackage);
		writeServiceImpl(entityClass, basePackage);
		writeMapper(entityClass, basePackage);
	}

	public static <T extends Serializable> void writeService(Class<T> entityClass, String basePackage) {
		SourcePathInfo sourcePathInfo = new SourcePathInfo(basePackage, entityClass, SouceType.SERVICE);
		final String data = render("com/td/framework/dao/mybatis/util/service.tmp", buildParam(entityClass, basePackage));
		write(sourcePathInfo, data);
	}

	public static <T extends Serializable> void writeServiceImpl(Class<T> entityClass, String basePackage) {
		SourcePathInfo sourcePathInfo = new SourcePathInfo(basePackage, entityClass, SouceType.SERVICE_IMPL);
		final String data = render("com/td/framework/dao/mybatis/util/service_impl.tmp", buildParam(entityClass, basePackage));
		write(sourcePathInfo, data);
	}

	public static <T extends Serializable> void writeMapper(Class<T> entityClass, String basePackage) {
		SourcePathInfo sourcePathInfo = new SourcePathInfo(basePackage, entityClass, SouceType.MAPPER);
		final String data = render("com/td/framework/dao/mybatis/util/mapper.tmp", buildParam(entityClass, basePackage));
		write(sourcePathInfo, data);
	}

}
