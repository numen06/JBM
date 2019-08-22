package jbm.framework.boot.autoconfigure.baidu.ocr;

import com.baidu.aip.ocr.AipOcr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BaiduOcrProperties.class)
//@ConditionalOnProperty(BaiduOcrProperties.PREFIX)
public class BaiduOcrAutoConfiguration {

    @Autowired
    private BaiduOcrProperties baiduOcrProperties;

    @Bean
    public AipOcr baiduOcr() {
        // 初始化一个AipOcr
        AipOcr client = new AipOcr(baiduOcrProperties.getAppId(), baiduOcrProperties.getApiKey(), baiduOcrProperties.getSecretKey());
        // 可选：设置网络连接参数
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);
        return client;
    }


}
