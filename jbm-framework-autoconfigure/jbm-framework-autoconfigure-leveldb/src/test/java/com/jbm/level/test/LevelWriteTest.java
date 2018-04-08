package com.jbm.level.test;

import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.level.LevelKeyValueTemplate;
import org.springframework.data.level.LevelTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import com.alibaba.fastjson.JSON;

import jbm.framework.boot.autoconfigure.level.LevelAutoConfiguration;

@RunWith(SpringRunner.class)
@SpringBootConfiguration
@SpringBootTest(classes = { LevelAutoConfiguration.class })
public class LevelWriteTest {
	@Autowired
	private LevelTemplate<Object, Object> levelTemplate;

	@Autowired
	private LevelKeyValueTemplate levelKeyValueTemplate;

//	@Test
	public void testWrite() {
		System.out.println(JSON.toJSONString(levelKeyValueTemplate.findById("123", UserTestBean.class)));
		System.out.println("start:" + new Date());
		for (int i = 0; i < 100000; i++) {
			// levelKeyValueTemplate.insert(ObjectId.get(),
			// UUID.randomUUID().toString());
			levelKeyValueTemplate.update("123", UserTestBean.newBean());
		}
		System.out.println(JSON.toJSONString(levelKeyValueTemplate.findById("123", UserTestBean.class)));
		System.out.println("end:" + new Date());
	}
	
	@Test
	public void testWrite2() {
		System.out.println(JSON.toJSONString(levelKeyValueTemplate.findById("1234", String.class)));
		System.out.println("start:" + new Date());
		for (int i = 0; i < 100000; i++) {
			// levelKeyValueTemplate.insert(ObjectId.get(),
			// UUID.randomUUID().toString());
			levelKeyValueTemplate.update("1234", "1231");
		}
		System.out.println(JSON.toJSONString(levelKeyValueTemplate.findById("1234", String.class)));
		System.out.println("end:" + new Date());
	}

}
