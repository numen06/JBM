package test;

import com.jbm.util.PasswordUtils;
import org.junit.Test;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-07-03 13:11
 **/
public class PasswordUtilsTest {

    @Test
    public void test() {
        System.out.println(PasswordUtils.checkPassword("1312"));
        System.out.println(PasswordUtils.checkPassword("h123123"));
        System.out.println(PasswordUtils.checkPassword("dad123123"));
        System.out.println(PasswordUtils.checkPassword("Adad123123"));
        System.out.println(PasswordUtils.checkPassword("A123&fa"));
        System.out.println(PasswordUtils.checkPassword("A123&afad"));
    }
}
