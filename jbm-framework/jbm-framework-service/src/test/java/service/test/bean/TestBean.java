package service.test.bean;

import java.lang.reflect.Field;

import javax.persistence.Transient;

import org.apache.commons.lang.reflect.FieldUtils;

import com.mchange.v2.naming.ReferenceableUtils;
import com.jbm.util.ClassUtils;
import com.jbm.util.ReflectionUtils;

import jodd.util.ReflectUtil;

/**
 * 
 * 缓存测试
 * 
 * @author wesley
 *
 */
public class TestBean {
	private String name;
	@Transient
	public String code;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public static void main(String[] args) {
		Field f = ReflectionUtils.findField(TestBean.class, "code");
		Transient t = f.getAnnotation(Transient.class);
		System.out.println(t);
	}
}
