package jbm.framework.beetl.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.beetl.core.ByteWriter;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.beetl.core.exception.BeetlException;
import org.beetl.core.resource.StringTemplateResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import com.github.pfmiles.org.apache.commons.lang.StringUtils;

/**
 * 模板操作类
 * 
 * @author wesley.zhang
 *
 */
public class BeetlTemplate {

	private static final Logger logger = LoggerFactory.getLogger(BeetlTemplate.class);

	private GroupTemplateFactaryBean groupTemplateFactaryBean;

	public BeetlTemplate(GroupTemplateFactaryBean groupTemplateFactaryBean) {
		super();
		this.groupTemplateFactaryBean = groupTemplateFactaryBean;
	}

	public BeetlTemplate() {
		super();
		this.groupTemplateFactaryBean = new GroupTemplateFactaryBean();
	}

	/**
	 * 获取原生的模板操作类
	 * 
	 * @return
	 * @throws IOException
	 */
	public GroupTemplate getGroupTemplate() throws IOException {
		return this.groupTemplateFactaryBean.getObject();
	}

	/**
	 * 通过文件目录下查找模板
	 * 
	 * @param key
	 * @return
	 * @throws IOException
	 */
	public Template getTemplate(String key) throws IOException {
		return groupTemplateFactaryBean.getObject().getTemplate(key);
	}

	/**
	 * 通过资源文件获取模板
	 * 
	 * @param resource
	 * @return
	 * @throws IOException
	 */
	public Template getTemplate(Resource resource) throws IOException {
		InputStream input = resource.getInputStream();
		String temp = IOUtils.toString(input);
		return getStringTemplate(temp);
	}

	/**
	 * 通过文件获取模板
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public Template getTemplate(File file) throws IOException {
		return getTemplate(new FileSystemResource(file));
	}

	/**
	 * 通过字符串直接获取模板
	 * 
	 * @param str
	 * @return
	 * @throws IOException
	 */
	public Template getStringTemplate(String str) throws IOException {
		if (StringUtils.isBlank(str)) {
			return null;
		}
		StringTemplateResourceLoader resourceLoader = new StringTemplateResourceLoader();
		return groupTemplateFactaryBean.getObject().getTemplate(str, resourceLoader);
	}

	/**
	 * 通过资源路径下的文件名编译模板
	 * 
	 * @param key
	 * @param map
	 * @return
	 */
	public <K, V> String render(String key, Map<K, V> map) {
		try {
			final Template temp = this.getTemplate(key);
			temp.binding(map);
			return temp.render();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 通过资源文件
	 * 
	 * @param resource
	 * @param map
	 * @return
	 */
	public <K, V> String render(Resource resource, Map<K, V> map) {
		try {
			final Template temp = this.getTemplate(resource);
			temp.binding(map);
			return temp.render();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 通过文件地址编译模板
	 * 
	 * @param file
	 * @param map
	 * @return
	 */
	public <K, V> String render(File file, Map<K, V> map) {
		try {
			final Template temp = this.getTemplate(file);
			temp.binding(map);
			return temp.render();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 通过字符串编译模板
	 * 
	 * @param str
	 * @param map
	 * @return
	 */
	public <K, V> String renderString(String str, Map<K, V> map) {
		try {
			final Template temp = this.getStringTemplate(str);
			temp.binding(map);
			return temp.render();
		} catch (Exception e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private <K, V, T> T renderAny(Template temp, Map<K, V> map, T output) throws BeetlException, IOException {
		temp.binding(map);
		if (output instanceof String) {
			return (T) temp.render();
		} else if (output instanceof OutputStream) {
			temp.renderTo((OutputStream) output);
		} else if (output instanceof Writer) {
			temp.renderTo((Writer) output);
		} else if (output instanceof ByteWriter) {
			temp.renderTo((ByteWriter) output);
		} else if (output instanceof File) {
			temp.renderTo(new FileWriter((File) output));
		}
		return output;
	}

	public <K, V, T> T render(File file, Map<K, V> map, T output) {
		try {
			return renderAny(this.getTemplate(file), map, output);
		} catch (Exception e) {
			logger.error("beetl render fail", e);
		}
		return null;
	}

	/**
	 * 通过资源路径下的文件名编译模板
	 * 
	 * @param key
	 * @param map
	 * @return
	 */
	public <K, V, T> T render(String key, Map<K, V> map, T output) {
		try {
			return renderAny(this.getTemplate(key), map, output);
		} catch (Exception e) {
			logger.error("beetl render fail", e);
		}
		return null;
	}

	/**
	 * 通过资源文件
	 * 
	 * @param resource
	 * @param map
	 * @return
	 */
	public <K, V, T> T render(Resource resource, Map<K, V> map, T output) {
		try {
			return renderAny(this.getTemplate(resource), map, output);
		} catch (Exception e) {
			logger.error("beetl render fail", e);
		}
		return null;
	}

	/**
	 * 通过字符串编译模板
	 * 
	 * @param str
	 * @param map
	 * @return
	 */
	public <K, V, T> T renderString(String str, Map<K, V> map, T output) {
		try {
			return renderAny(this.getStringTemplate(str), map, output);
		} catch (Exception e) {
			logger.error("beetl render fail", e);
		}
		return null;
	}

}
