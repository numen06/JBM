package com.jbm.test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.beetl.core.Template;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import jodd.io.FileUtil;
import jbm.framework.beetl.core.BeetlTemplate;
import jbm.framework.boot.autoconfigure.beetl.BeetlAutoConfiguration;

@RunWith(SpringRunner.class)
// @Import(BeetlProperties.class)
@SpringBootTest(classes = BeetlAutoConfiguration.class)
public class BeetlTest {

	@Autowired
	private BeetlTemplate beetlTemplate;

	@Test
	public void exampleTest() throws IOException {
		Template temp = beetlTemplate.getTemplate("test.txt");
		temp.binding("name", "wesley");
		System.out.println(temp.render());
	}

	@Test
	public void exampleTest2() throws IOException {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("name", "wesley2");
		String str = beetlTemplate.render("/root/test.txt", map, "");
		System.out.println(str);
	}

	@Test
	public void exampleTest3() throws IOException {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("name", "wesley3");
		File file = beetlTemplate.render("/root/test.txt", map, FileUtil.createTempFile());
		System.out.println(FileUtils.readFileToString(file));
	}

	@Test
	public void exampleTest4() throws IOException {
		Map<String, Object> map = new HashMap<String, Object>();
		File file = new File("target/test");
		FileUtils.forceMkdir(file);
		file = beetlTemplate.render("test1.xml", map, new File("target/test/test1.xls"));
		System.out.println(FileUtils.readFileToString(file));
	}
}
