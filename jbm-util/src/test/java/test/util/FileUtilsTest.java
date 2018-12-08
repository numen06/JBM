package test.util;

import com.jbm.util.FileUtils;
import junit.framework.TestCase;

public class FileUtilsTest extends TestCase {

    public void testMain(String[] args) {
//        FileUtils.forceMkdir("d:/test/test/test");

        try {
            System.out.println(FileUtils.getClassPathFile("test.txt"));
            System.out.println(FileUtils.exists("d:/test/test/test"));
        } catch (Exception e) {

        }
    }
}
