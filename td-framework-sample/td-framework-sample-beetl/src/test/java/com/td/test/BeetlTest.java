package com.td.test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.beetl.core.resource.FileResourceLoader;

import junit.framework.TestCase;

public class BeetlTest extends TestCase {

	public void testTemp() throws IOException {
		FileResourceLoader resourceLoader = new FileResourceLoader("D:/Documents/Tencent Files/261894663/FileRecv/");
		Configuration cfg = Configuration.defaultConfiguration();
		GroupTemplate gt = new GroupTemplate(resourceLoader, cfg);
		Template t = gt.getTemplate("测试企业_2017年04月月报20170525081922.xml");
		 t.binding("name", "beetl");
		FileOutputStream fos = new FileOutputStream("D:/Documents/Tencent Files/261894663/FileRecv/测试企业_2017年04月月报20170525081922-111.xls");
		t.renderTo(fos);
	}
}
