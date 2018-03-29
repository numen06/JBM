package test.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import com.td.util.ini.IniReader;

import jodd.io.FileUtil;
import jodd.props.Props;
import jodd.props.PropsEntry;
import jodd.util.ClassLoaderUtil;
import junit.framework.TestCase;

public class ClassLoaderUtilTest extends TestCase {

	public void testLoad() throws IOException {
		URL url = ClassLoaderUtil.getResourceUrl("test.ini");
		File containerFile = FileUtil.toContainerFile(url);
		System.out.println(FileUtil.readString(containerFile));
	}

	public void testPropLoad() throws IOException {
		System.out.println("----------testPropLoad----");
		URL url = ClassLoaderUtil.getResourceUrl("test.ini");
		File containerFile = FileUtil.toContainerFile(url);
		Props p = new Props();
		p.load(containerFile);
		Iterator<PropsEntry> list = p.entries().iterator();
		for (Iterator<PropsEntry> iterator = list; iterator.hasNext();) {
			PropsEntry type = iterator.next();
			System.out.println("key:" + type.getKey());
			System.out.println("value:" + type.getValue());
		}
		System.out.println("--------------");
	}

	public void testIni() throws IOException {
		System.out.println("----------ini----");
		URL url = ClassLoaderUtil.getResourceUrl("test.ini");
		File containerFile = FileUtil.toContainerFile(url);
		IniReader ini = new IniReader(containerFile);
		System.out.println(ini.getSections());
		for (String section : ini.getSections()) {
			Iterator<PropsEntry> list = ini.entries().section(section).iterator();
			for (Iterator<PropsEntry> iterator = list; iterator.hasNext();) {
				PropsEntry type = iterator.next();
				System.out.println("ini key:" + type.getKey());
				System.out.println("ini value:" + type.getValue());
			}
		}

		System.out.println("--------------");
	}

}
