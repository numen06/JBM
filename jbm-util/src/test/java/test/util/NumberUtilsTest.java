package test.util;

import com.jbm.util.NumberUtils;
import junit.framework.TestCase;

public class NumberUtilsTest extends TestCase {

    public void testMain() {
        System.out.println(NumberUtils.minimum(123123.1, 1d, 3.12312));
        System.out.println(NumberUtils.format(100000, "00.000"));
    }
}
