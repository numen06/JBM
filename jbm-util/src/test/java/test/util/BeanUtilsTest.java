package test.util;

import java.lang.reflect.InvocationTargetException;

import com.jbm.util.BeanUtils;

import junit.framework.TestCase;
import test.entity.Student;

public class BeanUtilsTest extends TestCase {

	public void testGetProperty() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Student st = new Student();
		st.setName("wesley");
		System.out.println(BeanUtils.getProperty(st, "name"));
	}
}
