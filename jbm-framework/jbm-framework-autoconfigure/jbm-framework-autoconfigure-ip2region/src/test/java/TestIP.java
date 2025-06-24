import cn.hutool.core.thread.ThreadUtil;
import jbm.framework.boot.autoconfigure.ip2region.IpRegionTemplate;
import org.junit.jupiter.api.Test;

/**
 * @program: JBM7
 * @author: wesley.zhang
 * @create: 2020-02-18 05:20
 **/
public class TestIP {

    @Test
    public void testIP() throws Exception {
        IpRegionTemplate ipRegionTemplate = new IpRegionTemplate();
        ipRegionTemplate.afterPropertiesSet();
        System.out.println(ipRegionTemplate.getRegion("180.162.26.193"));
        ThreadUtil.safeSleep(15000);

    }
    @Test
    public void testDownload() throws Exception {
        IpRegionTemplate ipRegionTemplate = new IpRegionTemplate();
        ipRegionTemplate.download(IpRegionTemplate.DB_URL,IpRegionTemplate.DB_PATH_TEMP);
    }
}
