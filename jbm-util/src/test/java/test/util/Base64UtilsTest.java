package test.util;

import com.jbm.util.Base64Utils;
import junit.framework.TestCase;

import java.io.IOException;

public class Base64UtilsTest extends TestCase {


    public void testMain() {
        String testStr = "dsfasdfasdfasdczxc爱笑的发的是";
        System.out.println("test:" + testStr);
        String base64 = Base64Utils.encodeSerializable(testStr);
        System.out.println("base64:" + base64);
        System.out.println("test2:" + Base64Utils.decodeSerializable(base64));
    }
}
