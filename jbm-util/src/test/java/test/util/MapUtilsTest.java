package test.util;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.junit.Test;

import test.entity.Student;

import com.jbm.util.MapUtils;

public class MapUtilsTest {

	@Test
	public void test() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Student bean = new Student();
		bean.setAge(1);
		Map<String, Object> map = MapUtils.toMapIgnoreNull(bean);
		System.out.print(map.toString());
	}

}
