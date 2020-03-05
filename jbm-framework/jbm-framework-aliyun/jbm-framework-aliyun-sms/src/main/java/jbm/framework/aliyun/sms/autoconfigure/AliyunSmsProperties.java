package jbm.framework.aliyun.sms.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-03-05 05:13
 **/
@Data
@ConfigurationProperties(prefix = "aliyun.sms")
public class AliyunSmsProperties {

    /**
     * 客户端ID
     */
    private String accessKeyId;
    /**
     * 客户端验证码
     */
    private String accessKeySecret;
    /**
     * 签名
     */
    private String signName;
    /**
     * 默认短信验证码模板
     */
    private String pinTemplateCode = "SMS_157645254";
}
