package jbm.framework.aliyun.sms;

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
import jbm.framework.aliyun.sms.autoconfigure.AliyunSmsProperties;
import jbm.framework.aliyun.sms.model.AliyunSms;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;

/**
 * @program: JBM7
 * @author: wesley.zhang
 * @create: 2020-03-05 05:09
 **/
@Slf4j
public class AliyunSmsTemplate {

    private final AliyunSmsProperties aliyunSmsProperties;
    private IAcsClient client;

    public AliyunSmsTemplate(AliyunSmsProperties aliyunSmsProperties) {
        this.aliyunSmsProperties = aliyunSmsProperties;
    }

    @PostConstruct
    public void init() {
        try {
            DefaultProfile profile = DefaultProfile.getProfile("default", aliyunSmsProperties.getAccessKeyId(), aliyunSmsProperties.getAccessKeySecret());
            client = new DefaultAcsClient(profile);
        } catch (Exception e) {
            log.error("初始化错误", e);
        }
    }

    private CommonRequest buildAliRequest() {
        CommonRequest request = new CommonRequest();
        //request.setProtocol(ProtocolType.HTTPS);
        request.setMethod(MethodType.POST);
        request.setDomain("dysmsapi.aliyuncs.com");
        request.setVersion("2017-05-25");
        request.setAction("SendSms");
        return request;
    }

    private CommonRequest buildRequest(AliyunSms aliyunSms) {
        CommonRequest request = buildAliRequest();
        return buildRequest(request, aliyunSms);
    }


    private CommonRequest buildRequest(CommonRequest request, AliyunSms aliyunSms) {
        request.putQueryParameter("PhoneNumbers", StringUtils.join(aliyunSms.getPhoneNumbers().toArray()));
        request.putQueryParameter("SignName", aliyunSms.getSignName() == null ? aliyunSmsProperties.getSignName() : aliyunSms.getSignName());
        request.putQueryParameter("TemplateCode", aliyunSms.getTemplateCode());
        request.putQueryParameter("TemplateParam", JSON.toJSONString(aliyunSms.getTemplateParam()));
        return request;
    }


    /**
     * 发送短信验证码
     *
     * @param pinCode
     * @param phoneNumbers
     * @return
     * @throws Exception
     */
    public JSONObject sendPin(String pinCode, String... phoneNumbers) throws Exception {
        AliyunSms aliyunSms = new AliyunSms();
        aliyunSms.setPhoneNumbers(Sets.newHashSet(phoneNumbers));
        aliyunSms.setTemplateCode(aliyunSmsProperties.getPinTemplateCode());
        aliyunSms.setTemplateParam(ImmutableMap.of("code", pinCode));
        return this.sendSms(aliyunSms);
    }


    /**
     * 发送短信
     *
     * @param aliyunSms
     * @return
     * @throws Exception
     */
    public JSONObject sendSms(AliyunSms aliyunSms) throws Exception {
        try {
            CommonRequest request = this.buildRequest(aliyunSms);
            CommonResponse response = client.getCommonResponse(request);
            log.info(response.getData());
            return JSON.parseObject(response.getData());
        } catch (ServerException e) {
            throw e;
        } catch (ClientException e) {
            throw e;
        }
    }
}
