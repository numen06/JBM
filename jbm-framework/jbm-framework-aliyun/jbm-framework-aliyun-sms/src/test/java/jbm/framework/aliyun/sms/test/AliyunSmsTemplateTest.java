package jbm.framework.aliyun.sms.test;

import jbm.framework.aliyun.sms.AliyunSmsTemplate;
import jbm.framework.aliyun.sms.autoconfigure.AliyunSmsProperties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

/**
 * @program: JBM7
 * @author: wesley.zhang
 * @create: 2020-03-05 05:09
 **/
@Slf4j
public class AliyunSmsTemplateTest {

    private static AliyunSmsTemplate aliyunSmsTemplate;

    static {
        try {
            AliyunSmsProperties aliyunSmsProperties = new AliyunSmsProperties();
//            aliyunSmsProperties.setAccessKeyId(" ");
//            aliyunSmsProperties.setAccessKeySecret(" ");
//            aliyunSmsProperties.setSignName(" ");
            aliyunSmsTemplate = new AliyunSmsTemplate(aliyunSmsProperties);
            aliyunSmsTemplate.init();
        } catch (Exception e) {
            log.error("初始化错误", e);
        }
    }


    @Test
    public void testPin() throws Exception {
        aliyunSmsTemplate.sendPin("12312", "13585658904");
    }

}
