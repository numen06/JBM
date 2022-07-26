package jbm.framework.boot.autoconfigure.ip2region;

import org.springframework.context.annotation.Bean;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-02-18 00:58
 **/
public class Ip2regionAutoConfiguration {

    /**
     * @return
     */
    @Bean
    public IpRegionTemplate ipRegionTemplate() {
        IpRegionTemplate ipRegionTemplate = new IpRegionTemplate();
        return ipRegionTemplate;
    }

}
