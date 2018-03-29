package test.util;

import java.util.Map;

import org.junit.Test;

import com.td.util.MapUtils;
import com.td.util.ScriptEngineUtils;

public class ScriptEngineUtilsTest {

	@Test
	public void test() {
		Map<String, Object> map = MapUtils.newParamMap();
		try {
			Object result = ScriptEngineUtils.callScript("test.js", "jsTest", map, 12312);
			System.out.println(result);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
