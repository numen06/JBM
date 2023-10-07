package test.util;

import com.jbm.util.IniUtils;
import com.jbm.util.ini.IniReader;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;

public class IniUtilsTest {
    public static final String FILENAME = "test.ini";

    @Test
    public void loadIniKey() throws IOException {
        String filename = FILENAME;
        IniReader prefs = IniUtils.loadClassPath(filename);
        System.out.println("grumpy/homePage: " + prefs.getValue("unit_type.energy"));
    }

    @Test
    public void loadSection() throws IOException, BackingStoreException {
        String filename = FILENAME;
        IniReader prefs = IniUtils.loadClassPath(filename);
        List<String> types = prefs.getSections();
        for (int i = 0; i < types.size(); i++) {
            String type = types.get(i);
            Map<String, String> nodes = prefs.getValues(type);
            for (String key : nodes.keySet()) {
                String val = nodes.get(key);
                System.out.println("key: " + key + "= type:" + val);
            }
        }

    }
}
