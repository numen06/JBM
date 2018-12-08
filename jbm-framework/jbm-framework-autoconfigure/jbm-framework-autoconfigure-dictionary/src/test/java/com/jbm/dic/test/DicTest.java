package com.jbm.dic.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.jbm.autoconfig.dic.DictionaryTemplate;

import jbm.framework.boot.autoconfigure.dictionary.DictionaryAutoConfiguration;

@RunWith(SpringRunner.class)
@SpringBootConfiguration
@SpringBootTest(classes = { DictionaryAutoConfiguration.class })
public class DicTest {

	@Autowired
	private DictionaryTemplate dictionaryCache;

	@Test
	public void exampleTest1() {
		System.out.println(dictionaryCache.getValue(DicTestBean3.class, "test0"));
		// System.out.println(dictionaryCache.getValue("name"));
	}

	@Test
	public void exampleTest2() {
		System.out.println(dictionaryCache.getValues(DicTestBean3.class));
//		System.out.println(dictionaryCache.getValues("test"));
	}
	
	@Test
	public void exampleTest3() {
		System.out.println(DicTestBean3.test0.eq("test"));
		System.out.println(DicTestBean3.test0.toJsonString());
		System.out.println(DicTestBean3.test0.toString());
//		System.out.println(dictionaryCache.getValues("test"));
	}

}
