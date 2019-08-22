package jbm.framework.boot.autoconfigure.baidu.ocr;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = BaiduOcrProperties.PREFIX)
public class BaiduOcrProperties {

    public static final String PREFIX = "baidu.ocr";

    private String appid;
    private String apiKey;
    private String secretKey;
    private String proxyHost;


}
