import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import com.alibaba.fastjson.JSON;
import jbm.framework.boot.autoconfigure.openobserve.OpenObserveProperties;
import jbm.framework.boot.autoconfigure.openobserve.OpenObserveTemplate;
import jbm.framework.boot.autoconfigure.openobserve.QueryResult;
import jbm.framework.boot.autoconfigure.openobserve.model.QueryBean;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Map;

public class OpenObserveTest {

   private static OpenObserveTemplate openObserveTemplate;

    @BeforeAll
    public static void init() {
        OpenObserveProperties openObserveProperties = new OpenObserveProperties();
        openObserveProperties.setBaseUrl("http://10.100.10.64:5080");
        openObserveProperties.setOrganization("default");
        openObserveProperties.setStream("test");
        openObserveProperties.setUsername("admin@example.com");
        openObserveProperties.setPassword("Admin#123");
        openObserveTemplate = new OpenObserveTemplate(openObserveProperties);
    }

    @Test
    public void testAdd() {
        String log = ResourceUtil.readUtf8Str("gateway_logs.json");
//        String log = ResourceUtil.readUtf8Str("test.json");
        openObserveTemplate.postLogs(log);
    }

    @Test
    public void select() {
        QueryBean queryBean = new QueryBean();
        queryBean.getQuery().setSql("SELECT * FROM test");
        queryBean.getQuery().setFrom(0);
        queryBean.getQuery().setSize(10);
        Date now = DateTime.now();
        queryBean.getQuery().setStartTime(DateUtil.offsetDay(now,-1).getTime()*1000);
        queryBean.getQuery().setEndTime(now.getTime()*1000);
        QueryResult queryResult =  openObserveTemplate.selectLogs(queryBean);
        if (queryResult.getTotal() > 0) {
            System.out.println(queryResult.getTotal());
        }
        for (Map<String, Object> hit : queryResult.getHits()) {
            System.out.println(hit);
        }
//        System.out.println(JSON.toJSONString(queryResult.getHits()));

    }
}
