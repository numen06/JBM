package test.util;

import java.util.Date;

import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import com.td.util.TimeUtil;

import test.entity.Student;

public class JsonTest {

	@Test
	public void testDate() {
		ParserConfig.getGlobalInstance().putDeserializer(Date.class, new DateDeserializer());
		Student test1 = JSON.parseObject("{\"time\":\"2017/11/02 10:50:56\"}", Student.class);
		System.out.println(test1.getTime());

		String dateStr = "2017/11/2 10:50:56";
		System.out.println(TimeUtil.softParseDate(dateStr));
	}
}
