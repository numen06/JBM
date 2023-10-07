package test.util;

import com.jbm.util.BeanUtils;
import junit.framework.TestCase;
import test.entity.Student;

import java.lang.reflect.InvocationTargetException;

public class BeanUtilsTest extends TestCase {

    public void testGetProperty() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Student st = new Student();
        st.setName("wesley");
        System.out.println(BeanUtils.getProperty(st, "name"));
    }
}
