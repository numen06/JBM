package jbm.framework.aliyun.sms.test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.jbm.util.StringUtils;
import jbm.framework.aliyun.sms.AliyunSmsTemplate;
import jbm.framework.aliyun.sms.autoconfigure.AliyunSmsProperties;
import jbm.framework.aliyun.sms.model.AliyunSms;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.PostConstruct;

/**
 * @program: JBM6
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
