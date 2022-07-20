package jbm.framework.boot.autoconfigure.baidu;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = BaiduProperties.PREFIX)
public class BaiduProperties {

    public static final String PREFIX = "baidu.api";
    @ApiModelProperty(value = "应用ID")
    private String appid;
    @ApiModelProperty(value = "应用KEY")
    private String apiKey;
    @ApiModelProperty(value = "应用密钥")
    private String secretKey;
    @ApiModelProperty(value = "代理地址")
    private String proxyHost;
    @ApiModelProperty(value = "连接超时")
    private Integer connectionTimeout = 2000;
    @ApiModelProperty(value = "通讯超时")
    private Integer socketTimeout = 60000;


}
