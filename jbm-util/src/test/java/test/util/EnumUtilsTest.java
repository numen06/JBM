package test.util;

import com.jbm.util.EnumUtils;
import junit.framework.TestCase;
import test.entity.TestEnum;

public class EnumUtilsTest extends TestCase {

    public void testEqualsIgnoreCase() {
        System.out.println(EnumUtils.equalsIgnoreCase(TestEnum.test, "test1"));
    }


    public void testContains() {
        System.out.println(EnumUtils.contains(TestEnum.class, "test1"));
        System.out.println(EnumUtils.contains(TestEnum.class, "test2"));
        System.out.println(EnumUtils.contains(TestEnum.class, "test3"));
    }
}
