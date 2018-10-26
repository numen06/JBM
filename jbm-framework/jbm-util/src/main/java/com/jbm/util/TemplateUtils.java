package com.jbm.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import com.github.pfmiles.minvelocity.TemplateUtil;
import com.github.pfmiles.org.apache.velocity.Template;

/**
 * 模板工具类
 * 
 * @author wesley
 *
 */
public class TemplateUtils extends TemplateUtil {
	/**
	 * 
	 * 通过文件生成执行模板
	 * 
	 * @param tempFile
	 *            模板文件
	 * @param ctxPojo
	 *            模板上下文对象，可为pojo也可为map
	 * @return 渲染模板，直接返回渲染好的string
	 * @throws IOException
	 */
	public static <T> String renderTemplate(File tempFile, T ctxPojo) throws IOException {
		String templateString = FileUtils.readFileToString(tempFile);
		Template temp = TemplateUtil.parseStringTemplate(templateString);
		return TemplateUtil.renderTemplate(temp, ctxPojo);
	}

	public static <T> String renderTemplate(InputStream inputStream, T ctxPojo) throws IOException {
		String templateString = IOUtils.toString(inputStream);
		IOUtils.closeQuietly(inputStream);
		Template temp = TemplateUtil.parseStringTemplate(templateString);
		return TemplateUtil.renderTemplate(temp, ctxPojo);
	}
}
