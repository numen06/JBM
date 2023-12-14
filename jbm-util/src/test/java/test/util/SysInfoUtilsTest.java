package test.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.jbm.util.SysInfoUtils;
import org.junit.jupiter.api.Test;

public class SysInfoUtilsTest {

    @Test
    public void testMonitorInfo() {
        System.out.println(JSON.toJSONString(SysInfoUtils.getMonitorInfoOshi(), SerializerFeature.PrettyFormat));
    }

    @Test
    public void testDiskInfo() {
        System.out.println(JSON.toJSONString(SysInfoUtils.getDiskInfo(), SerializerFeature.PrettyFormat));
    }

    @Test
    public void testNetInfo() {
        System.out.println(JSON.toJSONString(SysInfoUtils.getNetworkInfo(), SerializerFeature.PrettyFormat));
    }
}
