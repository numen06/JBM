package test.util;

import com.jbm.util.MapUtils;
import org.junit.Test;
import test.entity.Student;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class MapUtilsTest {

    @Test
    public void test() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Student bean = new Student();
        bean.setAge(1);
        Map<String, Object> map = MapUtils.toMapIgnoreNull(bean);
        System.out.print(map.toString());
    }

}
