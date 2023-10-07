package test.util;

import com.google.common.collect.Lists;
import com.jbm.util.MapUtils;
import com.jbm.util.StringUtils;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

public class StringUtilsTest {

    @Test
    public void beanTrimToNull() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
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

    @Test
    public void splitToEmpty() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        // System.out.print(StringUtils.exchangeType("20141112", Date.class) +
        // "");
        System.out.println(System.nanoTime());
        System.out.println("tes" + "123123213");
        System.out.println(System.nanoTime());
        System.out.println(StringUtils.bond("tes", "123123213"));
        System.out.println(System.nanoTime());

        System.out.println(StringUtils.splitToEmpty("1").toString());

        System.out.println(Arrays.toString(StringUtils.splitToArray("201501,201511", Date.class)));
        System.out.println(StringUtils.collectionToDelimitedString(Lists.newArrayList("1", "2"), ";"));
        System.out.println(StringUtils.toString(MapUtils.newHashMap("1", "2")));
    }

}
