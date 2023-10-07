package test.util;

import com.jbm.util.JavaBeanUtil;
import org.junit.Test;

public class JavaBeanUtilsTest {

    @Test
    public void testAll() {
        System.out.println(JavaBeanUtil.toUnderlineString("ISOCertifiedStaff"));
        System.out.println(JavaBeanUtil.getValidPropertyName("CertifiedStaff"));
        System.out.println(JavaBeanUtil.getSetterMethodName("userID"));
        System.out.println(JavaBeanUtil.getGetterMethodName("userID"));
        System.out.println(JavaBeanUtil.toCamelCaseString("iso_certified_staff", true));
        System.out.println(JavaBeanUtil.getValidPropertyName("certified_staff"));
        System.out.println(JavaBeanUtil.toCamelCaseString("site_Id"));
    }
}
