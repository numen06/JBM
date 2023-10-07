package test.util;

import com.jbm.util.PathUtils;
import org.junit.Test;

import java.io.File;
import java.util.UUID;

public class PathUtilsTest {


    @Test
    public void testAll() {
        UUID id = UUID.randomUUID();
        System.out.println(PathUtils.randomFileName(""));
        System.out.println(id.toString().replace("-", ""));

        System.out.println(PathUtils.getExtension(".12"));
        System.out.println(PathUtils.getStandardExtension("1s2"));
        System.out.println(PathUtils.getStandardExtension("123123.te"));
        String fileName = "D:/apache-tomcat-7.0.23/webapps/ROOT/report rets/attachment/report/test/212.11111/1";
        System.out.println("----");
        System.out.println(PathUtils.isFile(fileName));
        System.out.println(PathUtils.fillExtension(fileName, "PDF"));
        System.out.println(PathUtils.getWithoutExtension(PathUtils.fillExtension(fileName, "PDF")));
        System.out.println(PathUtils.getFileName(fileName));
        System.out.println(new File(fileName).getPath());
        String[] paths = {"win", "t", "a", "teswe"};
        System.out.println(PathUtils.bulidFullPath(paths));

        System.out.println(PathUtils.rename("d:/1/1/13/./test.test", "1231.11"));
    }
}
