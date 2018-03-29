package com.td.dic.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.td.autoconfig.dic.DictionaryCache;

import td.framework.boot.autoconfigure.dictionary.DictionaryAutoConfiguration;

@RunWith(SpringRunner.class)
@SpringBootConfiguration
@SpringBootTest(classes = { DictionaryAutoConfiguration.class })
public class DicTest {

	@Autowired
	private DictionaryCache dictionaryCache;

	@Test
	public void exampleTest1() {
		System.out.println(dictionaryCache.getInteger(PileDealStatusDict.OffLine));
		// System.out.println(dictionaryCache.getValue("name"));
	}

	@Test
	public void exampleTest2() {
		System.out.println(dictionaryCache.getInteger(PileDealStatusDict.OnLineFree));
//		System.out.println(dictionaryCache.getValues("test"));
	}

}
