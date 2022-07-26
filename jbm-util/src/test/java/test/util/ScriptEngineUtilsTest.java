package test.util;

import com.jbm.util.MapUtils;
import com.jbm.util.ScriptEngineUtils;
import org.junit.Test;

import java.util.Map;

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
