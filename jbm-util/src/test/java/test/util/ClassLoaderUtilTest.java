package test.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.setting.Setting;
import com.jbm.util.ini.IniReader;
import junit.framework.TestCase;
import org.apache.commons.io.Charsets;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

public class ClassLoaderUtilTest extends TestCase {

    public void testLoad() throws IOException {
        URL url = ResourceUtil.getResource("test.ini");
        File containerFile = FileUtil.file(url);
        System.out.println(FileUtil.readUtf8String(containerFile));
    }

    public void testPropLoad() throws IOException {
        System.out.println("----------testPropLoad----");
        URL url = ResourceUtil.getResource("test.ini");
        File containerFile = FileUtil.file(url);
        Setting setting = new Setting(containerFile, Charsets.UTF_8, true);
        Properties p = setting.toProperties();
        for (String key : p.stringPropertyNames()) {
            System.out.println("key:" + key);
            System.out.println("value:" + p.get(key));
        }
        System.out.println("--------------");
    }

    public void testIni() throws IOException {
        System.out.println("----------ini----");
        URL url = ResourceUtil.getResource("test.ini");
        File containerFile = FileUtil.file(url);
        IniReader ini = new IniReader(containerFile);
        System.out.println(ini.getSections());
        for (String section : ini.getSections()) {
            for (String key : ini.getValues(section).keySet()) {
                System.out.println("ini key:" + key);
                System.out.println("ini value:" + ini.getValue(section, key));
            }
        }

        System.out.println("--------------");
    }

}
