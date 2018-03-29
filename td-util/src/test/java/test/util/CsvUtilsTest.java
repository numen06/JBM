package test.util;

import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.td.util.CsvUtils;

import junit.framework.TestCase;

public class CsvUtilsTest extends TestCase {
	public void testRowToObject() {
		System.out.println(JSON.toJSONString(CsvUtils.toObject("hzlbl110967,20161112,951,452640", Map.class, "name", "date")));
		System.out.println(JSON.toJSONString(CsvUtils.toObject("hzlbl110967,20161112,951,452640", Map.class, "name", "date", null, "d2", "d3")));
	}
}
