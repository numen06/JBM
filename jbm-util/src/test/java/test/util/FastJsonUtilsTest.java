package test.util;

import cn.hutool.core.io.resource.ResourceUtil;
import com.jbm.util.FastJsonUtils;
import junit.framework.TestCase;

/**
 * @author: create by wesley
 * @date:2019/5/12
 */
public class FastJsonUtilsTest extends TestCase {

    public void testToSnakeCase() {
        String str = ResourceUtil.readUtf8Str("campinfo.json");
        System.out.println(FastJsonUtils.toSnakeCase(str));
    }

    public void testToUpperCamel() {
        String str = ResourceUtil.readUtf8Str("campinfo.json");
        System.out.println(FastJsonUtils.toUpperCamel(str));
    }
}
