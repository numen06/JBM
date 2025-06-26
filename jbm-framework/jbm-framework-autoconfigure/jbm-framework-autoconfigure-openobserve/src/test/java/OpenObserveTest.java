import cn.hutool.core.io.resource.ResourceUtil;
import jbm.framework.boot.autoconfigure.openobserve.OpenObserveProperties;
import jbm.framework.boot.autoconfigure.openobserve.OpenObserveTemplate;
import jbm.framework.boot.autoconfigure.openobserve.model.QueryBean;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class OpenObserveTest {

   private static OpenObserveTemplate openObserveTemplate;

    @BeforeAll
    public static void init() {
        OpenObserveProperties openObserveProperties = new OpenObserveProperties();
        openObserveProperties.setBaseUrl("http://10.100.10.64:5080");
        openObserveProperties.setOrganization("default");
        openObserveProperties.setStream("test");
        openObserveProperties.setUsername("admin@example.com");
        openObserveProperties.setPassword("3BDG1Suys4emDlpP");
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
        queryBean.getQuery().setStartTime(0L);
        queryBean.getQuery().setEndTime(System.currentTimeMillis());
        openObserveTemplate.selectLogs(queryBean);


    }
}
