package test.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import com.jbm.util.TimeUtils;
import org.junit.Test;
import test.entity.Student;

import java.util.Date;

public class JsonTest {

    @Test
    public void testDate() {
        ParserConfig.getGlobalInstance().putDeserializer(Date.class, new DateDeserializer());
        Student test1 = JSON.parseObject("{\"time\":\"2017/11/02 10:50:56\"}", Student.class);
        System.out.println(test1.getTime());

        String dateStr = "2017/11/2 10:50:56";
        System.out.println(TimeUtils.softParseDate(dateStr));
    }
}
