package test.util;

import com.jbm.util.ConvertUtils;
import com.jbm.util.TimeUtils;
import org.junit.Test;

import java.util.Date;

public class ConverUtilsTest {

    @Test
    public void test() throws Exception {
        System.out.println(ConvertUtils.converts("2013-01-01", Date.class, TimeUtils.now()));
    }
}
