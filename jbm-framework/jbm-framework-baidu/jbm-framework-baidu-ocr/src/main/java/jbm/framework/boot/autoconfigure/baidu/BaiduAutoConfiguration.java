package jbm.framework.boot.autoconfigure.baidu;

import com.baidu.aip.face.AipFace;
import com.baidu.aip.ocr.AipOcr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author wesley
 */
@Configuration
@EnableConfigurationProperties(BaiduProperties.class)
@ConditionalOnProperty(prefix = BaiduProperties.PREFIX, name = {"appid"})
public class BaiduAutoConfiguration {

    @Autowired
    private BaiduProperties baiduProperties;

    @Bean
    public AipOcr aipOcr() {
        // 初始化一个AipOcr
        AipOcr client = new AipOcr(baiduProperties.getAppid(), baiduProperties.getApiKey(), baiduProperties.getSecretKey());
        // 可选：设置网络连接参数
        client.setConnectionTimeoutInMillis(baiduProperties.getConnectionTimeout());
        client.setSocketTimeoutInMillis(baiduProperties.getSocketTimeout());
        return client;
    }

    @Bean
    public AipFace aipFace() {
        // 初始化一个AipOcr
        AipFace aipFace = new AipFace(baiduProperties.getAppid(), baiduProperties.getApiKey(), baiduProperties.getSecretKey());
        // 可选：设置网络连接参数
        aipFace.setConnectionTimeoutInMillis(baiduProperties.getConnectionTimeout());
        aipFace.setSocketTimeoutInMillis(baiduProperties.getSocketTimeout());
        return aipFace;
    }


}
