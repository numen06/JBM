package com.jbm.micro.sb.test;

import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.alibaba.fastjson.JSONObject;
import com.jbm.framework.masterdata.usage.paging.PageForm;
import com.jbm.framework.mvc.web.SpringBootWebTest;
import com.jbm.sample.entity.AggregateData;
import com.jbm.sample.mysql.MySqlAppStart;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MySqlAppStart.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MySqlAppStartTest extends SpringBootWebTest {

	@Test
	public void save() throws Exception {
		AggregateData data = new AggregateData();
		data.setData(1.0);
		logger.info("测试结果1为：{}", this.requestJson("save", entityToModel(data)));
//		AggregateData data2 = new AggregateData();
//		data2.setData(2.0);
//		logger.info("测试结果2为：{}", this.requestJson("save", entityToModel(data2)));
//		AggregateData data3 = new AggregateData();
//		data3.setData(3.0);
//		logger.info("测试结果3为：{}", this.requestJson("save", entityToModel(data3)));
	}

	@Test
	public void pageList() throws Exception {
		PageForm pageForm = new PageForm(1, 3);
		logger.info("测试结果为：{}", this.requestJson("pageList", entityToModel(pageForm)));
	}

	@Test
	public void list() throws Exception {
		logger.info("测试结果为：{}", this.requestJson("list", ""));
	}

	@Test
	public void selectEntityMapByCodes() throws Exception {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("codes", Lists.newArrayList(1, 2, 3));
		logger.info("测试结果为：{}", this.requestJson("selectEntityMapByCodes", jsonObject));
	}

	@Test
	public void mapperList() throws Exception {
		PageForm pageForm = new PageForm(1, 5, "dataType:desc");
		JSONObject jsonObject = entityToModel(pageForm);
//		jsonObject.put("id", 1064924176006995970l);
		logger.info("测试结果为：{}", this.requestJson("mapperList", jsonObject));
	}

	@Test
	public void mapperList2() throws Exception {
//		PageForm pageForm = new PageForm(1, 3);
//		JSONObject jsonObject = entityToModel(pageForm);
//		jsonObject.put("id", 1064924176006995970l);
//		logger.info("测试结果为：{}", this.requestJson("mapperList2", jsonObject));
		logger.info("测试结果为：{}", this.requestJson("mapperList2", ""));
	}
}
