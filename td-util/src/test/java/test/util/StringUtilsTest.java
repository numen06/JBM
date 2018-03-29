package test.util;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import com.td.util.MapUtils;
import com.td.util.StringUtils;

public class StringUtilsTest {

	public static void main(String[] args) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Map<String, String> stu = MapUtils.newHashMap();
		stu.put("test", "     ");
		System.out.println(System.currentTimeMillis());
		StringUtils.beanTrimToNull(stu);
		System.out.println(System.currentTimeMillis());
		stu.put("test", "   12312 ");
		System.out.println(System.currentTimeMillis());
		StringUtils.beanTrimToNull(stu);
		System.out.println(System.currentTimeMillis());
		// System.out.println(JSON.serialize(stu));
	}

}
