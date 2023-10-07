package test.util;

import com.alibaba.fastjson.JSON;
import com.jbm.util.CsvUtils;
import junit.framework.TestCase;

import java.util.Map;

public class CsvUtilsTest extends TestCase {
    public void testRowToObject() {
        System.out.println(JSON.toJSONString(CsvUtils.toObject("hzlbl110967,20161112,951,452640", Map.class, "name", "date")));
        System.out.println(JSON.toJSONString(CsvUtils.toObject("hzlbl110967,20161112,951,452640", Map.class, "name", "date", null, "d2", "d3")));
    }
}
