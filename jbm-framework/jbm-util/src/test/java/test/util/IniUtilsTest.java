package test.util;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.BackingStoreException;

import org.junit.Test;

import com.jbm.util.IniUtils;
import com.jbm.util.ini.IniReader;

import jodd.props.PropsEntry;

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
			Iterator<PropsEntry> keys = prefs.getValues(type);
			for (Iterator<PropsEntry> iterator = keys; iterator.hasNext();) {
				PropsEntry node = iterator.next();
				String key = node.getKey();
				System.out.println("key: " + key + "= type:" + node.getValue());
			}
		}

	}
}
