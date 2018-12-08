package test.util;

import com.jbm.util.ArrayUtils;
import com.jbm.util.ListUtils;
import junit.framework.TestCase;

import java.util.Date;
import java.util.List;

public class ArrayUtilsTest extends TestCase {

    public void testMain() {
        List<String> s = ListUtils.newArrayList("sdfasd", "asdfsad", "asdfsad", null);
        List<Date> dd = ListUtils.newArrayList(new Date(), new Date(), null);
        String[] ss = s.toArray(new String[0]);
        System.out.println(ArrayUtils.toSimpleString(ss));
        System.out.println(ListUtils.toString(s, ""));
        System.out.println(ListUtils.toSimpleString(dd));
        System.out.println(ArrayUtils.toSimpleString(null));
        System.out.println(ArrayUtils.toSimpleString(null, "---"));
    }
}
